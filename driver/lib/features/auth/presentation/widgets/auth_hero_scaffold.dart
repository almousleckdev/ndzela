import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_icon_button.dart';

// The frame of the sign-in screens: the car on top, and a white sheet with rounded corners rising over it that holds the title and the form. With the keyboard open the car shrinks and the sheet ends above the keyboard, so the fields stay in view.
class AuthHeroScaffold extends StatelessWidget {
  const AuthHeroScaffold({
    super.key,
    required this.title,
    required this.subtitle,
    required this.children,
    this.showBack = false,
  });

  final String title;
  final String subtitle;
  final List<Widget> children;
  final bool showBack;

  static const _radius = Radius.circular(28);
  static const _slide = Duration(milliseconds: 220);

  @override
  Widget build(BuildContext context) {
    final screen = MediaQuery.sizeOf(context);
    final top = MediaQuery.paddingOf(context).top;
    final keyboard = MediaQuery.viewInsetsOf(context).bottom;
    final photoHeight = keyboard > 0
        ? top + 72
        : min(max(screen.height * 0.34, 190.0), 320.0);
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: AppColors.canvas,
        resizeToAvoidBottomInset: false,
        body: Stack(
          children: [
            AnimatedPositioned(
              duration: _slide,
              top: 0,
              left: 0,
              right: 0,
              height: photoHeight + 40,
              child: Image.asset(
                'assets/images/image.png',
                fit: BoxFit.cover,
                cacheWidth: 1200,
                excludeFromSemantics: true,
              ),
            ),
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              height: top + 40,
              child: const DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [Color(0x33000000), Color(0x00000000)],
                  ),
                ),
              ),
            ),
            if (showBack)
              Positioned(
                top: top + 8,
                left: 12,
                child: DecoratedBox(
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                  ),
                  child: AppIconButton(
                    icon: AppIcons.back,
                    tooltip: 'Retour',
                    onPressed: () => context.pop(),
                  ),
                ),
              ),
            AnimatedPositioned(
              duration: _slide,
              top: photoHeight,
              left: 0,
              right: 0,
              bottom: keyboard,
              child: DecoratedBox(
                decoration: const BoxDecoration(
                  color: AppColors.canvas,
                  borderRadius: BorderRadius.vertical(top: _radius),
                ),
                child: SingleChildScrollView(
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  padding: EdgeInsets.fromLTRB(
                    20,
                    12,
                    20,
                    24 +
                        (keyboard > 0
                            ? 0
                            : MediaQuery.paddingOf(context).bottom),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Center(
                        child: Container(
                          width: 40,
                          height: 4,
                          decoration: BoxDecoration(
                            color: AppColors.line,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Text(
                        title,
                        textAlign: TextAlign.center,
                        style: AppTextStyles.title,
                      ),
                      const SizedBox(height: 6),
                      Text(
                        subtitle,
                        textAlign: TextAlign.center,
                        style: AppTextStyles.label,
                      ),
                      const SizedBox(height: 22),
                      ...children,
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
