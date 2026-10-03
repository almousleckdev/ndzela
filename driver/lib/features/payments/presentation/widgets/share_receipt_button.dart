import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/error_messages.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../../domain/payment_attempt.dart';
import '../receipt_share_controller.dart';

// "Partager le reçu"
class ShareReceiptButton extends ConsumerWidget {
  const ShareReceiptButton({super.key, required this.attempt});

  final PaymentAttempt attempt;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sharing = ref.watch(receiptShareControllerProvider);
    ref.listen(receiptShareControllerProvider, (_, next) {
      if (next.hasError) showAppMessage(context, messageFor(next.error!));
    });
    return AppButton(
      label: 'Partager le reçu',
      icon: AppIcons.share,
      variant: AppButtonVariant.secondary,
      loading: sharing.isLoading,
      onPressed: () =>
          ref.read(receiptShareControllerProvider.notifier).share(attempt),
    );
  }
}
