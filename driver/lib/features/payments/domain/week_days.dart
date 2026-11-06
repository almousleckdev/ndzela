import 'rent_day.dart';

// The rent days of one week, Monday to Saturday (Sunday carries no rent); a slot is empty when no rent was due that day.
class WeekDays {
  const WeekDays({required this.start, required this.days});

  static const rentDaysPerWeek = 6;

  final DateTime start;
  final List<RentDay?> days;

  int get paid => _count(
    (d) => d == RentDayStatus.paidOnTime || d == RentDayStatus.paidLate,
  );
  int get unpaid => _count((d) => d == RentDayStatus.unpaid);
  int get awaiting => _count((d) => d == RentDayStatus.awaiting);

  DateTime get end => start.add(const Duration(days: 5));

  int _count(bool Function(RentDayStatus status) test) =>
      days.where((d) => d != null && test(d.status)).length;

  // The last `weeks` weeks ending with the one of `now`, newest first
  static List<WeekDays> lastWeeks(
    List<RentDay> rentDays,
    DateTime now, {
    int weeks = 4,
  }) {
    final thisMonday = _monday(now);
    return [
      for (var i = 0; i < weeks; i++)
        _week(thisMonday.subtract(Duration(days: 7 * i)), rentDays),
    ];
  }

  static WeekDays _week(DateTime start, List<RentDay> rentDays) {
    final byDate = {for (final d in rentDays) _dateOnly(d.date): d};
    return WeekDays(
      start: start,
      days: [
        for (var i = 0; i < rentDaysPerWeek; i++)
          byDate[_dateOnly(start.add(Duration(days: i)))],
      ],
    );
  }

  static DateTime _monday(DateTime date) =>
      DateTime(date.year, date.month, date.day - (date.weekday - 1));

  static DateTime _dateOnly(DateTime date) =>
      DateTime(date.year, date.month, date.day);
}
