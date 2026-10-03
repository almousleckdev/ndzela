import 'package:intl/intl.dart';

// "1 octobre 2026"
String formatDate(DateTime date) => DateFormat('d MMMM y', 'fr').format(date);

// "36 mois", "1 mois"
String formatMonths(int months) => '$months mois';

// "30 sept.
String formatDateShort(DateTime date) =>
    DateFormat('d MMM y', 'fr').format(date);

// "12 oct.
String formatDateTime(DateTime date) =>
    DateFormat('d MMM y, HH:mm', 'fr').format(date);
