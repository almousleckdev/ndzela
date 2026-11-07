import 'dart:typed_data';

import 'package:driver/app/splash_hold.dart';
import 'memory_storage.dart';
import 'mocks.dart';
import 'package:dio/dio.dart';
import 'package:driver/app/app.dart';
import 'package:driver/core/money/money.dart';
import 'package:driver/core/platform/document_picker.dart';
import 'package:driver/core/platform/open_file.dart';
import 'package:driver/core/platform/share_file.dart';
import 'package:driver/core/storage/token_storage.dart';
import 'package:driver/features/auth/presentation/auth_providers.dart';
import 'package:driver/features/contract/presentation/contract_providers.dart';
import 'package:driver/features/payments/domain/arrears_summary.dart';
import 'package:driver/features/payments/domain/payment_attempt.dart';
import 'package:driver/features/payments/domain/payment_operator.dart';
import 'package:driver/features/payments/domain/payment_request.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/presentation/payments_providers.dart';
import 'package:driver/features/profile/presentation/profile_providers.dart';
import 'package:driver/features/support/presentation/support_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/misc.dart' show Override;
import 'package:mocktail/mocktail.dart';
import 'sample_data.dart';

class FakeBackend {
  FakeBackend({
    this.arrears = const ArrearsSummary(
      unpaidDays: 3,
      total: Money(69000, 'XAF'),
    ),
    this.signedIn = false,
    this.mustChangePassword = false,
    this.photo,
    this.finalStatus = PaymentStatus.success,
    this.history = const [],
  }) {
    registerFallbackValue(
      const PaymentRequest(
        operator: PaymentOperator(code: '', label: ''),
        phone: '',
      ),
    );
    if (signedIn) {
      storage.seed({
        'access_token': 'a',
        'refresh_token': 'r',
        'must_change_password': '$mustChangePassword',
      });
    }
    when(() => profile.fetchMe()).thenAnswer((_) async => sampleProfile);
    when(() => profile.fetchPhoto()).thenAnswer((_) async => photo);
    when(
      () => profile.fetchDocuments(),
    ).thenAnswer((_) async => sampleDocuments);
    when(
      () => profile.fetchContractPdf(),
    ).thenAnswer((_) async => sampleReceipt);
    when(() => contract.fetchDetails()).thenAnswer((_) async => sampleContract);
    when(() => support.fetchContacts()).thenAnswer((_) async => sampleSupport);
    when(() => support.sendMessage(any())).thenAnswer((_) async {});
    when(() => support.requestAccountDeletion()).thenAnswer((_) async {});
    when(() => contract.fetchVehicle()).thenAnswer((_) async => sampleVehicle);
    when(() => payments.fetchArrears()).thenAnswer((_) async => arrears);
    when(
      () => payments.fetchOperators(),
    ).thenAnswer((_) async => sampleOperators);
    when(() => payments.start(any())).thenAnswer((_) async => pendingAttempt);
    when(
      () => payments.fetchAttempt(any()),
    ).thenAnswer((_) async => finishedAttempt);
    when(() => payments.fetchHistory()).thenAnswer((_) async => history);
    when(
      () => payments.fetchReceipt(any()),
    ).thenAnswer((_) async => sampleReceipt);
    when(
      () => payments.quote(
        operatorCode: any(named: 'operatorCode'),
        phone: any(named: 'phone'),
        days: any(named: 'days'),
      ),
    ).thenAnswer((_) async => sampleQuote);
    when(
      () => auth.signIn(
        identifier: any(named: 'identifier'),
        password: any(named: 'password'),
      ),
    ).thenAnswer(
      (_) => tokens.save(
        access: 'a',
        refresh: 'r',
        mustChangePassword: mustChangePassword,
      ),
    );
    when(
      () => auth.changePassword(
        currentPassword: any(named: 'currentPassword'),
        newPassword: any(named: 'newPassword'),
      ),
    ).thenAnswer(
      (_) =>
          tokens.save(access: 'a2', refresh: 'r2', mustChangePassword: false),
    );
    when(() => auth.signOut()).thenAnswer((_) async {});
    when(() => auth.requestResetCode(any())).thenAnswer((_) async {});
    when(
      () => auth.verifyResetCode(
        identifier: any(named: 'identifier'),
        code: any(named: 'code'),
      ),
    ).thenAnswer((_) async {});
    when(
      () => auth.resetPassword(
        identifier: any(named: 'identifier'),
        code: any(named: 'code'),
        newPassword: any(named: 'newPassword'),
      ),
    ).thenAnswer((_) async {});
  }

  final ArrearsSummary arrears;
  final bool signedIn;
  final bool mustChangePassword;
  final Uint8List? photo;
  final PaymentStatus finalStatus;
  final List<PaymentAttempt> history;

  late final pendingAttempt = PaymentAttempt(
    id: 'p1',
    status: PaymentStatus.pending,
    amount: const Money(69000, 'XAF'),
    operatorCode: 'MOOV_MONEY',
    phone: '074111111',
    reference: 'NDZREF123456',
    createdAt: DateTime(2026, 10, 1, 14, 30),
  );

  late final finishedAttempt = PaymentAttempt(
    id: 'p1',
    status: finalStatus,
    amount: const Money(69000, 'XAF'),
    fee: finalStatus == PaymentStatus.success ? const Money(2415, 'XAF') : null,
    operatorCode: 'MOOV_MONEY',
    phone: '074111111',
    reference: 'NDZREF123456',
    beneficiary: 'NDZELA GABON',
    providerReference: finalStatus == PaymentStatus.success
        ? 'PAY0110261325109'
        : null,
    createdAt: DateTime(2026, 10, 1, 14, 30),
    completedAt: DateTime(2026, 10, 1, 14, 31),
  );
  final storage = MemoryStorage();
  late final tokens = TokenStorage(storage);
  final auth = MockAuthRepository();
  final profile = MockProfileRepository();
  final contract = MockContractRepository();
  final payments = MockPaymentsRepository();
  final support = MockSupportRepository();

  // What the phone's share sheet was asked to send
  final shared = <({String filename, String mimeType, int size})>[];

  // What the phone's file picker hands back (null: the driver chose nothing)
  PickedDocument? pickedDocument = PickedDocument(
    filename: 'permis.pdf',
    bytes: Uint8List.fromList([1, 2, 3]),
  );

  // The files shown in the phone's viewer
  final opened = <({String filename, int size})>[];
}

DioException apiError(int status, String code) {
  final request = RequestOptions(path: '/x');
  return DioException(
    requestOptions: request,
    response: Response(
      requestOptions: request,
      statusCode: status,
      data: {'code': code},
    ),
  );
}

Widget appWith(
  FakeBackend backend, {
  List<Override> overrides = const [],
  Duration splashMinimum = Duration.zero,
}) {
  return ProviderScope(
    overrides: [
      splashMinimumProvider.overrideWithValue(splashMinimum),
      ...overrides,
      tokenStorageProvider.overrideWithValue(backend.tokens),
      authRepositoryProvider.overrideWithValue(backend.auth),
      profileRepositoryProvider.overrideWithValue(backend.profile),
      contractRepositoryProvider.overrideWithValue(backend.contract),
      paymentsRepositoryProvider.overrideWithValue(backend.payments),
      supportRepositoryProvider.overrideWithValue(backend.support),
      documentPickerProvider.overrideWithValue(
        () async => backend.pickedDocument,
      ),
      fileOpenerProvider.overrideWithValue(({
        required bytes,
        required filename,
      }) async {
        backend.opened.add((filename: filename, size: bytes.length));
      }),
      fileSharerProvider.overrideWithValue(({
        required bytes,
        required filename,
        required mimeType,
        subject,
      }) async {
        backend.shared.add((
          filename: filename,
          mimeType: mimeType,
          size: bytes.length,
        ));
      }),
    ],
    child: const NdzelaApp(),
  );
}
