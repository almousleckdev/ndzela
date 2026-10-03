// Digits only; the server applies the country's exact rules.
bool isValidMobileMoneyNumber(String digits) =>
    RegExp(r'^\d{8,15}$').hasMatch(digits);
