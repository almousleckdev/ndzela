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

  // Ends the session locally; a reason says the server ended it.
  Future<void> signOut({SessionEndReason? reason}) async {
    await ref.read(tokenStorageProvider).clear();
    state = SignedOut(reason: reason);
  }

  // The driver's own sign-out
  Future<void> logout() async {
    await ref.read(authRepositoryProvider).signOut();
    await signOut();
  }

  // The login screen shows why the session ended once, then clears the reason.
  void acknowledgeSessionEnd() {
    if (state case SignedOut(reason: != null)) state = const SignedOut();
  }
}

final sessionControllerProvider = NotifierProvider<SessionController, Session>(
  SessionController.new,
);
