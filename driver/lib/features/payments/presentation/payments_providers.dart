import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../data/payments_repository.dart';
import '../domain/arrears_summary.dart';
import '../domain/payment_attempt.dart';
import '../domain/payment_operator.dart';
import '../domain/payment_quote.dart';

final paymentsRepositoryProvider = Provider<PaymentsRepository>(
  (ref) => PaymentsRepository(ref.watch(dioProvider)),
);

final arrearsProvider = FutureProvider<ArrearsSummary>(
  (ref) => ref.watch(paymentsRepositoryProvider).fetchArrears(),
);

final operatorsProvider = FutureProvider<List<PaymentOperator>>(
  (ref) => ref.watch(paymentsRepositoryProvider).fetchOperators(),
);

final paymentHistoryProvider = FutureProvider<List<PaymentAttempt>>(
  (ref) => ref.watch(paymentsRepositoryProvider).fetchHistory(),
);

// One payment, read again from the server (it may have finished since the list was loaded).
final paymentAttemptProvider = FutureProvider.autoDispose
    .family<PaymentAttempt, String>(
      (ref, id) => ref.watch(paymentsRepositoryProvider).fetchAttempt(id),
    );

// The fee and the amount for one operator and number; asked again when either changes.
final paymentQuoteProvider = FutureProvider.autoDispose
    .family<PaymentQuote, ({String operator, String phone})>(
      (ref, key) => ref
          .watch(paymentsRepositoryProvider)
          .quote(operatorCode: key.operator, phone: key.phone),
    );
