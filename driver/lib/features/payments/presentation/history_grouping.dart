import 'package:intl/intl.dart';

// How the history is cut into groups: by day, by payment week (Monday to Sunday) or by month.
enum HistoryGrouping {
  day('Jour'),
  week('Semaine'),
  month('Mois');

  const HistoryGrouping(this.label);

  final String label;

  // The first day of the group a moment belongs to
  DateTime start(DateTime at) => switch (this) {
    day => DateTime(at.year, at.month, at.day),
    week => DateTime(at.year, at.month, at.day - (at.weekday - 1)),
    month => DateTime(at.year, at.month),
  };

  String title(DateTime start, DateTime now) {
    switch (this) {
      case day:
        final today = DateTime(now.year, now.month, now.day);
        if (start == today) return "Aujourd'hui";
        if (start == DateTime(today.year, today.month, today.day - 1)) {
          return 'Hier';
        }
        return _capital(DateFormat('EEEE d MMMM y', 'fr').format(start));
      case week:
        final end = DateTime(start.year, start.month, start.day + 6);
        final from = DateFormat('d MMM', 'fr').format(start);
        return 'Semaine du $from au ${DateFormat('d MMM y', 'fr').format(end)}';
      case month:
        return _capital(DateFormat('MMMM y', 'fr').format(start));
    }
  }

  static String _capital(String text) =>
      text[0].toUpperCase() + text.substring(1);
}
