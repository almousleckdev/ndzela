import '../domain/payment_attempt.dart';
import '../domain/payment_status.dart';

// Which payments the history shows.
enum HistoryFilter {
  all('Tous'),
  confirmed('Confirmés'),
  other('Échoués ou expirés');

  const HistoryFilter(this.label);

  final String label;

  bool matches(PaymentAttempt attempt) => switch (this) {
    all => true,
    confirmed => attempt.status == PaymentStatus.success,
    other =>
      attempt.status == PaymentStatus.failed ||
          attempt.status == PaymentStatus.expired,
  };
}
