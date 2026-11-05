import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_icon_button.dart';
import '../../../../core/widgets/layout/screen_scaffold.dart';

// The frame of every step of the account screens: title band, optional back button, padded form.
class AuthStepScaffold extends StatelessWidget {
  const AuthStepScaffold({
    super.key,
    required this.title,
    required this.subtitle,
    required this.children,
    this.showBack = true,
  });

  final String title;
  final String subtitle;
  final List<Widget> children;
  final bool showBack;

  @override
  Widget build(BuildContext context) {
    return ScreenScaffold(
      title: title,
      subtitle: subtitle,
      leading: showBack
          ? AppIconButton(
              icon: AppIcons.back,
              tooltip: 'Retour',
              onPressed: () => context.pop(),
            )
          : null,
      body: SafeArea(
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: children,
          ),
        ),
      ),
    );
  }
}
