import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_providers.dart';
import 'session_controller.dart';

class ChangePasswordController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<void> change({
    required String currentPassword,
    required String newPassword,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(authRepositoryProvider)
          .changePassword(
            currentPassword: currentPassword,
            newPassword: newPassword,
          ),
    );
    if (!state.hasError) {
      await ref.read(sessionControllerProvider.notifier).sync();
    }
  }
}

final changePasswordControllerProvider =
    AsyncNotifierProvider.autoDispose<ChangePasswordController, void>(
      ChangePasswordController.new,
    );
