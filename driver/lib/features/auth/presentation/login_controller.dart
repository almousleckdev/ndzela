import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_providers.dart';
import 'session_controller.dart';

class LoginController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<void> signIn({
    required String identifier,
    required String password,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(authRepositoryProvider)
          .signIn(identifier: identifier.trim(), password: password),
    );
    if (!state.hasError) {
      await ref.read(sessionControllerProvider.notifier).sync();
    }
  }
}

final loginControllerProvider =
    AsyncNotifierProvider.autoDispose<LoginController, void>(
      LoginController.new,
    );
