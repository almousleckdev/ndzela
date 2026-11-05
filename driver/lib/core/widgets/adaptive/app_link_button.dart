import 'package:flutter/material.dart';

import '../../../app/theme/app_text_styles.dart';
import '../../../app/theme/brand_colors.dart';
import 'app_tappable.dart';

// A text-only action such as "Mot de passe oublié ?"
class AppLinkButton extends StatelessWidget {
  const AppLinkButton({super.key, required this.label, required this.onTap});

  final String label;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final enabled = onTap != null;
    return Semantics(
      button: true,
      enabled: enabled,
      label: label,
      child: AppTappable(
        onTap: onTap ?? () {},
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Text(
            label,
            style: AppTextStyles.action.copyWith(
              color: enabled ? context.brand.primaryInk : Colors.grey,
            ),
          ),
        ),
      ),
    );
  }
}
