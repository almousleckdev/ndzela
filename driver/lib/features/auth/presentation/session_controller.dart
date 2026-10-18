import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/storage/token_storage.dart';
import '../domain/session.dart';
import 'auth_providers.dart';

class SessionController extends Notifier<Session> {
  @override
  Session build() {
    sync();
    return const SessionLoading();
  }

  // Rebuilds the session from what is stored
  Future<void> sync() async {
    final tokens = ref.read(tokenStorageProvider);
    if (await tokens.readAccess() == null) {
      state = const SignedOut();
    } else {
      state = await tokens.readMustChangePassword()
          ? const PasswordChangeRequired()
          : const SignedIn();
    }
  }

  // Ends the session locally; also used when the server rejects the token (forced: true).
  Future<void> signOut({bool forced = false}) async {
    await ref.read(tokenStorageProvider).clear();
    state = SignedOut(forced: forced);
  }

  // The driver's own sign-out
  Future<void> logout() async {
    await ref.read(authRepositoryProvider).signOut();
    await signOut();
  }

  // The login screen shows the "signed out elsewhere" message once, then clears the flag.
  void acknowledgeForcedSignOut() {
    if (state case SignedOut(forced: true)) state = const SignedOut();
  }
}

final sessionControllerProvider = NotifierProvider<SessionController, Session>(
  SessionController.new,
);
