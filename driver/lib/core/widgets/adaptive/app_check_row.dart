import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_text_styles.dart';
import '../../../app/theme/brand_colors.dart';
import '../../platform/platform.dart';

// A labelled on/off choice: a checkbox on Android, a switch on iOS
class AppCheckRow extends StatelessWidget {
  const AppCheckRow({
    super.key,
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final bool value;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    final cupertino = isCupertino(context);
    return Semantics(
      container: true,
      label: label,
      toggled: value,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onChanged == null ? null : () => onChanged!(!value),
        child: Row(
          children: [
            if (!cupertino)
              SizedBox(
                width: 28,
                height: 28,
                child: Checkbox(
                  value: value,
                  onChanged: onChanged == null ? null : (v) => onChanged!(v!),
                  activeColor: context.brand.primary,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              ),
            if (!cupertino) const SizedBox(width: 8),
            Expanded(child: Text(label, style: AppTextStyles.label)),
            if (cupertino)
              Transform.scale(
                scale: 0.8,
                child: CupertinoSwitch(
                  value: value,
                  onChanged: onChanged,
                  activeTrackColor: context.brand.primary,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
