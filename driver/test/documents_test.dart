import 'dart:typed_data';

import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/profile/domain/document_kind.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/phone_view.dart';

void main() {
  setUpAll(() {
    registerFallbackValue(DocumentKind.licence);
    registerFallbackValue(Uint8List(0));
  });

  Future<FakeBackend> openProfile(WidgetTester tester) async {
    usePhone(tester, height: 7000);
    final backend = FakeBackend(signedIn: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(3));
    await tester.pumpAndSettle();
    return backend;
  }

  testWidgets('a missing document can be added from the phone', (tester) async {
    final backend = await openProfile(tester);
    when(
      () => backend.profile.addDocument(
        kind: any(named: 'kind'),
        filename: any(named: 'filename'),
        bytes: any(named: 'bytes'),
      ),
    ).thenAnswer((_) async {});

    expect(find.text('Ajouter'), findsOneWidget);
    await tester.tap(find.text('Ajouter'));
    await tester.pumpAndSettle();

    final sent = verify(
      () => backend.profile.addDocument(
        kind: captureAny(named: 'kind'),
        filename: captureAny(named: 'filename'),
        bytes: any(named: 'bytes'),
      ),
    ).captured;
    expect(sent[1], 'permis.pdf');
    expect(find.text('Document ajouté. Merci.'), findsOneWidget);
  });

  testWidgets('choosing nothing sends nothing', (tester) async {
    final backend = await openProfile(tester);
    backend.pickedDocument = null;

    await tester.tap(find.text('Ajouter'));
    await tester.pumpAndSettle();

    verifyNever(
      () => backend.profile.addDocument(
        kind: any(named: 'kind'),
        filename: any(named: 'filename'),
        bytes: any(named: 'bytes'),
      ),
    );
  });

  testWidgets('a document already on file has no add button', (tester) async {
    await openProfile(tester);

    expect(
      find.text('Ajouter'),
      findsOneWidget,
      reason: 'only the one missing document',
    );
  });
}
