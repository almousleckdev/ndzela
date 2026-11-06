import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';

// The white band at the top of every screen; it runs under the status bar and ends on a thin line.
class HeaderBand extends StatelessWidget {
  const HeaderBand({super.key, required this.child, this.bottomPadding = 12});

  final Widget child;
  final double bottomPadding;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        border: const Border(bottom: BorderSide(color: AppColors.line)),
      ),
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          16,
          MediaQuery.paddingOf(context).top + 8,
          16,
          bottomPadding,
        ),
        child: child,
      ),
    );
  }
}
