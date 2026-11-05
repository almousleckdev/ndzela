import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';

// A phone-sized surface; lists build lazily, so screens with long content pass a taller `height`.
void usePhone(WidgetTester tester, {double height = 4800}) {
  tester.view.physicalSize = Size(1080, height);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
}
