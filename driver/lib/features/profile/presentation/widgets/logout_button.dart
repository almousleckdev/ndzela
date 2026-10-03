import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../auth/presentation/confirm_logout.dart';

class LogoutButton extends ConsumerWidget {
  const LogoutButton({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AppButton(
      label: 'Se déconnecter',
      icon: AppIcons.logout,
      variant: AppButtonVariant.destructive,
      onPressed: () => confirmLogout(context, ref),
    );
  }
}
