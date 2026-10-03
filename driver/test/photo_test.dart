import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:driver/core/widgets/display/app_avatar.dart';
import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/profile/data/profile_repository.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';
import 'support/tiny_png.dart';

class _MockDio extends Mock implements Dio {}

void main() {
  setUpAll(() => registerFallbackValue(Options()));

  Finder photoImages() =>
      find.descendant(of: find.byType(AppAvatar), matching: find.byType(Image));

  testWidgets('the home avatar shows the driver photo when there is one', (
    tester,
  ) async {
    usePhone(tester);
    await tester.pumpWidget(
      appWith(FakeBackend(signedIn: true, photo: tinyPng)),
    );
    await tester.pumpAndSettle();

    expect(photoImages(), findsOneWidget);
    expect(find.text('AN'), findsNothing);
  });

  testWidgets('without a photo the avatar shows the initials', (tester) async {
    usePhone(tester);
    await tester.pumpWidget(appWith(FakeBackend(signedIn: true)));
    await tester.pumpAndSettle();

    expect(photoImages(), findsNothing);
    expect(find.text('AN'), findsOneWidget);
  });

  testWidgets('the profile header shows the photo too', (tester) async {
    usePhone(tester, height: 7000);
    await tester.pumpWidget(
      appWith(FakeBackend(signedIn: true, photo: tinyPng)),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(3));
    await tester.pumpAndSettle();

    expect(photoImages(), findsOneWidget);
  });

  testWidgets('a photo that cannot be decoded falls back to the initials', (
    tester,
  ) async {
    usePhone(tester);
    await tester.pumpWidget(
      appWith(
        FakeBackend(signedIn: true, photo: Uint8List.fromList([1, 2, 3])),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('AN'), findsOneWidget);
  });

  group('ProfileRepository.fetchPhoto', () {
    final dio = _MockDio();
    final repository = ProfileRepository(dio);

    DioException failure(int status) {
      final request = RequestOptions(path: '/me/photo');
      return DioException(
        requestOptions: request,
        response: Response(requestOptions: request, statusCode: status),
      );
    }

    test('returns the bytes', () async {
      when(
        () => dio.get<List<int>>('/me/photo', options: any(named: 'options')),
      ).thenAnswer(
        (_) async => Response(
          requestOptions: RequestOptions(path: '/me/photo'),
          data: tinyPng.toList(),
        ),
      );
      expect(await repository.fetchPhoto(), tinyPng);
    });

    test('returns null when the driver has no photo', () async {
      when(
        () => dio.get<List<int>>('/me/photo', options: any(named: 'options')),
      ).thenThrow(failure(404));
      expect(await repository.fetchPhoto(), isNull);
    });

    test('passes any other failure on', () async {
      when(
        () => dio.get<List<int>>('/me/photo', options: any(named: 'options')),
      ).thenThrow(failure(500));
      expect(repository.fetchPhoto(), throwsA(isA<DioException>()));
    });
  });
}
