import 'package:driver/core/money/money.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('money is grouped the French way and shown in FCFA', () {
    expect(const Money(69000, 'XAF').toString(), '69 000 FCFA');
    expect(const Money(23000, 'XAF') * 3, const Money(69000, 'XAF'));
  });
}
