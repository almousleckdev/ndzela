import 'package:driver/core/widgets/navigation/nav_item.dart';
import 'package:driver/features/auth/presentation/login_screen.dart';
import 'package:driver/features/support/domain/support_contacts.dart';
import 'package:driver/features/support/presentation/support_message_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'support/fakes.dart';
import 'support/open_message.dart';
import 'support/phone_view.dart';

void main() {
  Future<FakeBackend> openProfile(WidgetTester tester) async {
    usePhone(tester, height: 7000);
    final backend = FakeBackend(signedIn: true);
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(3));
    await tester.pumpAndSettle();
    return backend;
  }

  testWidgets('the personal information comes from the server', (tester) async {
    await openProfile(tester);

    expect(find.text('Informations personnelles'), findsOneWidget);
    expect(find.text('Awa Nze'), findsOneWidget);
    expect(find.text('awa@ndzela.test'), findsOneWidget);
    expect(find.text('12 mai 1990'), findsOneWidget);
    expect(find.text('Rue 12, Akanda, Libreville'), findsOneWidget);
  });

  testWidgets(
    'the contract section shows number, start, duration and daily rent',
    (tester) async {
      await openProfile(tester);

      expect(find.text('Détails du contrat'), findsOneWidget);
      expect(find.text('NDZ-77836447'), findsWidgets);
      expect(find.text('1 octobre 2026'), findsOneWidget);
      expect(find.text('36 mois'), findsOneWidget);
      expect(find.text('23 000 FCFA'), findsWidgets);
    },
  );

  testWidgets('the documents show which ones were handed in', (tester) async {
    await openProfile(tester);

    expect(find.text('Contrat de location'), findsOneWidget);
    expect(find.text('Casier judiciaire'), findsOneWidget);
    expect(find.text('Disponible'), findsNWidgets(2));
    expect(find.text('Manquant'), findsOneWidget);
  });

  testWidgets(
    'the driver can ask for the deletion of the account after confirming',
    (tester) async {
      final backend = await openProfile(tester);

      expect(find.text('Politique de confidentialité'), findsOneWidget);
      await tester.tap(find.text('Supprimer mon compte'));
      await tester.pumpAndSettle();
      expect(find.text('Supprimer mon compte ?'), findsOneWidget);
      verifyNever(() => backend.support.requestAccountDeletion());

      await tester.tap(find.text('Envoyer la demande'));
      await tester.pumpAndSettle();
      verify(() => backend.support.requestAccountDeletion()).called(1);
    },
  );

  testWidgets('the contract can be opened and downloaded from the documents', (
    tester,
  ) async {
    final backend = await openProfile(tester);

    await tester.tap(find.byTooltip('Ouvrir le contrat'));
    await tester.pumpAndSettle();
    verify(() => backend.profile.fetchContractPdf()).called(1);
    expect(backend.opened.single.filename, 'contrat-ndzela.pdf');

    await tester.tap(find.byTooltip('Télécharger le contrat'));
    await tester.pumpAndSettle();
    expect(backend.shared.single.filename, 'contrat-ndzela.pdf');
    expect(backend.shared.single.mimeType, 'application/pdf');
  });

  testWidgets('the support section offers the channels the country has set', (
    tester,
  ) async {
    await openProfile(tester);

    expect(find.text('Appeler'), findsOneWidget);
    expect(find.text('WhatsApp'), findsOneWidget);
    expect(find.text('E-mail'), findsWidgets);
    expect(find.text('Envoyer un message'), findsNothing);
  });

  testWidgets('a channel the country has not set is not offered', (
    tester,
  ) async {
    usePhone(tester, height: 7000);
    final backend = FakeBackend(signedIn: true);
    when(() => backend.support.fetchContacts()).thenAnswer(
      (_) async => const SupportContacts(email: 'ndzelagabon@gmail.com'),
    );
    await tester.pumpWidget(appWith(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(NavItem).at(3));
    await tester.pumpAndSettle();

    expect(find.text('Appeler'), findsNothing);
    expect(find.text('WhatsApp'), findsNothing);
  });

  testWidgets(
    'a message is validated, sent to the server and the driver comes back to the profile',
    (tester) async {
      final backend = await openProfile(tester);

      await openMessageScreen(tester);

      await tester.tap(find.widgetWithText(FilledButton, 'Envoyer'));
      await tester.pumpAndSettle();
      expect(find.text('Écrivez votre message.'), findsOneWidget);
      verifyNever(() => backend.support.sendMessage(any()));

      await tester.enterText(
        find.byType(TextFormField),
        '  Ma voiture fait un bruit.  ',
      );
      await tester.tap(find.widgetWithText(FilledButton, 'Envoyer'));
      await tester.pumpAndSettle();

      verify(
        () => backend.support.sendMessage('Ma voiture fait un bruit.'),
      ).called(1);
      expect(find.byType(SupportMessageScreen), findsNothing);
    },
  );

  testWidgets('a refused message shows the server reason and keeps the text', (
    tester,
  ) async {
    final backend = await openProfile(tester);
    when(
      () => backend.support.sendMessage(any()),
    ).thenThrow(apiError(422, 'TOO_MANY_MESSAGES'));

    await openMessageScreen(tester);
    await tester.enterText(find.byType(TextFormField), 'Bonjour');
    await tester.tap(find.widgetWithText(FilledButton, 'Envoyer'));
    await tester.pumpAndSettle();

    expect(
      find.text('Trop de messages envoyés. Réessayez dans une heure.'),
      findsOneWidget,
    );
    expect(find.byType(SupportMessageScreen), findsOneWidget);
  });

  testWidgets('logout asks for confirmation; cancelling keeps the session', (
    tester,
  ) async {
    final backend = await openProfile(tester);

    await tester.tap(find.widgetWithText(FilledButton, 'Se déconnecter'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsOneWidget);

    await tester.tap(find.text('Annuler'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsNothing);
    verifyNever(() => backend.auth.signOut());
  });

  testWidgets(
    'confirming the logout ends the session on the server and returns to login',
    (tester) async {
      final backend = await openProfile(tester);

      await tester.tap(find.widgetWithText(FilledButton, 'Se déconnecter'));
      await tester.pumpAndSettle();
      await tester.tap(
        find.descendant(
          of: find.byType(AlertDialog),
          matching: find.text('Se déconnecter'),
        ),
      );
      await tester.pumpAndSettle();

      verify(() => backend.auth.signOut()).called(1);
      expect(find.byType(LoginScreen), findsOneWidget);
    },
  );
}
