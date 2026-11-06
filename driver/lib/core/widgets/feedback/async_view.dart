import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../error/error_messages.dart';
import '../../platform/app_icons.dart';
import '../adaptive/app_button.dart';
import 'empty_state.dart';
import 'screen_skeleton.dart';

// Renders an AsyncValue with the app's loading and error states; `onRetry` re-runs the provider.
class AsyncView<T> extends StatelessWidget {
  const AsyncView({
    super.key,
    required this.value,
    required this.data,
    required this.onRetry,
  });

  final AsyncValue<T> value;
  final Widget Function(T data) data;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return value.when(
      data: data,
      loading: () => const ScreenSkeleton(cards: 1),
      error: (error, _) => Column(
        children: [
          EmptyState(icon: AppIcons.offline, title: messageFor(error)),
          SizedBox(
            width: 200,
            child: AppButton(label: 'Réessayer', onPressed: onRetry),
          ),
        ],
      ),
    );
  }
}
