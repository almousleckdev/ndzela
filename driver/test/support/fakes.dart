import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:driver/app/app.dart';
import 'package:driver/core/money/money.dart';
import 'package:driver/core/platform/share_file.dart';
import 'package:driver/core/storage/token_storage.dart';
import 'package:driver/features/auth/data/auth_repository.dart';
import 'package:driver/features/auth/presentation/auth_providers.dart';
import 'package:driver/features/contract/data/contract_repository.dart';
import 'package:driver/features/contract/domain/vehicle_info.dart';
import 'package:driver/features/contract/presentation/contract_providers.dart';
import 'package:driver/features/payments/data/payments_repository.dart';
import 'package:driver/features/payments/domain/arrears_summary.dart';
import 'package:driver/features/payments/domain/payment_attempt.dart';
import 'package:driver/features/payments/domain/payment_operator.dart';
import 'package:driver/features/payments/domain/payment_quote.dart';
import 'package:driver/features/payments/domain/payment_status.dart';
import 'package:driver/features/payments/domain/payment_request.dart';
import 'package:driver/features/payments/presentation/payments_providers.dart';
import 'package:driver/features/profile/data/profile_repository.dart';
import 'package:driver/features/profile/domain/document_kind.dart';
import 'package:driver/features/profile/domain/driver_document.dart';
import 'package:driver/features/support/data/support_repository.dart';
import 'package:driver/features/support/domain/support_contacts.dart';
import 'package:driver/features/support/presentation/support_providers.dart';
import 'package:driver/features/contract/domain/contract_details.dart';
import 'package:driver/features/profile/domain/driver_profile.dart';
import 'package:driver/features/profile/presentation/profile_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mocktail/mocktail.dart';

import 'memory_storage.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

class MockProfileRepository extends Mock implements ProfileRepository {}

class MockContractRepository extends Mock implements ContractRepository {}

class MockPaymentsRepository extends Mock implements PaymentsRepository {}

class MockSupportRepository extends Mock implements SupportRepository {}

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
    when(() => profile.fetchMe()).thenAnswer(
      (_) async => DriverProfile(
        firstName: 'Awa',
        lastName: 'Nze',
        phone: '+24174111111',
        localPhone: '074111111',
        email: 'awa@ndzela.test',
        birthDate: DateTime(1990, 5, 12),
        address: 'Rue 12, Akanda, Libreville',
      ),
    );
    when(() => profile.fetchPhoto()).thenAnswer((_) async => photo);
    when(() => profile.fetchDocuments()).thenAnswer(
      (_) async => const [
        DriverDocument(kind: DocumentKind.contract, available: true),
        DriverDocument(kind: DocumentKind.licence, available: true),
        DriverDocument(kind: DocumentKind.idCard, available: true),
        DriverDocument(kind: DocumentKind.addressProof, available: false),
      ],
    );
    when(() => contract.fetchDetails()).thenAnswer(
      (_) async => ContractDetails(
        number: 'NDZ-77836447',
        startDate: DateTime(2026, 10, 1),
        endDate: DateTime(2029, 9, 30),
        durationMonths: 36,
        dailyRent: const Money(23000, 'XAF'),
      ),
    );
    when(() => support.fetchContacts()).thenAnswer(
      (_) async => const SupportContacts(
        phone: '+24162821855',
        whatsapp: '+24162821855',
        email: 'ndzelagabon@gmail.com',
      ),
    );
    when(() => support.sendMessage(any())).thenAnswer((_) async {});
    when(() => support.requestAccountDeletion()).thenAnswer((_) async {});
    when(() => contract.fetchVehicle()).thenAnswer(
      (_) async => const VehicleInfo(name: 'Toyota Yaris', plate: 'AB-123-CD'),
    );
    when(() => payments.fetchArrears()).thenAnswer((_) async => arrears);
    when(() => payments.fetchOperators()).thenAnswer(
      (_) async => const [
        PaymentOperator(code: 'AIRTEL_MONEY', label: 'Airtel Money'),
        PaymentOperator(code: 'MOOV_MONEY', label: 'Moov Money'),
      ],
    );
    when(() => payments.start(any())).thenAnswer((_) async => pendingAttempt);
    when(
      () => payments.fetchAttempt(any()),
    ).thenAnswer((_) async => finishedAttempt);
    when(() => payments.fetchHistory()).thenAnswer((_) async => history);
    when(
      () => payments.fetchReceipt(any()),
    ).thenAnswer((_) async => Uint8List.fromList([0x25, 0x50, 0x44, 0x46]));
    when(
      () => payments.quote(
        operatorCode: any(named: 'operatorCode'),
        phone: any(named: 'phone'),
        days: any(named: 'days'),
      ),
    ).thenAnswer(
      (_) async => const PaymentQuote(
        amount: Money(69000, 'XAF'),
        fee: Money(2415, 'XAF'),
        accountHolder: 'Awa Nze',
      ),
    );
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

Widget appWith(FakeBackend backend) {
  return ProviderScope(
    overrides: [
      tokenStorageProvider.overrideWithValue(backend.tokens),
      authRepositoryProvider.overrideWithValue(backend.auth),
      profileRepositoryProvider.overrideWithValue(backend.profile),
      contractRepositoryProvider.overrideWithValue(backend.contract),
      paymentsRepositoryProvider.overrideWithValue(backend.payments),
      supportRepositoryProvider.overrideWithValue(backend.support),
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
