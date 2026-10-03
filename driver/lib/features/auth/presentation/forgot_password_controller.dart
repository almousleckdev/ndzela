import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_providers.dart';

// The two steps of a forgotten password
class ForgotPasswordController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<bool> requestCode(String identifier) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () =>
          ref.read(authRepositoryProvider).requestResetCode(identifier.trim()),
    );
    return !state.hasError;
  }

  Future<bool> reset({
    required String identifier,
    required String code,
    required String newPassword,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(authRepositoryProvider)
          .resetPassword(
            identifier: identifier.trim(),
            code: code.trim(),
            newPassword: newPassword,
          ),
    );
    return !state.hasError;
  }
}

final forgotPasswordControllerProvider =
    AsyncNotifierProvider.autoDispose<ForgotPasswordController, void>(
      ForgotPasswordController.new,
    );
