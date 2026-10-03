import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'support_providers.dart';

// Sends the driver's request to have their account and data deleted; the staff handle it.
class AccountDeletionController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<bool> request() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(supportRepositoryProvider).requestAccountDeletion(),
    );
    return !state.hasError;
  }
}

final accountDeletionControllerProvider =
    AsyncNotifierProvider.autoDispose<AccountDeletionController, void>(
      AccountDeletionController.new,
    );
