import 'package:flutter/widgets.dart';

import '../../../app/theme/app_colors.dart';

class ToastBanner extends StatelessWidget {
  const ToastBanner({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: MediaQuery.paddingOf(context).top + 12,
      left: 16,
      right: 16,
      child: IgnorePointer(
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: AppColors.ink,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: DefaultTextStyle(
              style: const TextStyle(
                color: Color(0xFFFFFFFF),
                fontSize: 13,
                fontWeight: FontWeight.w500,
              ),
              child: Text(message),
            ),
          ),
        ),
      ),
    );
  }
}
