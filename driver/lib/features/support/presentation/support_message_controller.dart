import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'support_providers.dart';

class SupportMessageController extends AsyncNotifier<void> {
  @override
  Future<void> build() async {}

  Future<bool> send(String message) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(supportRepositoryProvider).sendMessage(message.trim()),
    );
    return !state.hasError;
  }
}

final supportMessageControllerProvider =
    AsyncNotifierProvider.autoDispose<SupportMessageController, void>(
      SupportMessageController.new,
    );
