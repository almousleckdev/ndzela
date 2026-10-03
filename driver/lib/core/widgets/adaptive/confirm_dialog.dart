import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../../../app/theme/app_colors.dart';
import '../../platform/platform.dart';

// Asks the user to confirm an action; resolves to true only when they confirm.
Future<bool> showConfirmDialog(
  BuildContext context, {
  required String title,
  required String message,
  required String confirmLabel,
  String cancelLabel = 'Annuler',
  bool destructive = false,
}) async {
  final confirmed = isCupertino(context)
      ? await showCupertinoDialog<bool>(
          context: context,
          builder: (dialog) => CupertinoAlertDialog(
            title: Text(title),
            content: Text(message),
            actions: [
              CupertinoDialogAction(
                onPressed: () => Navigator.pop(dialog, false),
                child: Text(cancelLabel),
              ),
              CupertinoDialogAction(
                isDestructiveAction: destructive,
                isDefaultAction: !destructive,
                onPressed: () => Navigator.pop(dialog, true),
                child: Text(confirmLabel),
              ),
            ],
          ),
        )
      : await showDialog<bool>(
          context: context,
          builder: (dialog) => AlertDialog(
            title: Text(title),
            content: Text(message),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialog, false),
                child: Text(cancelLabel),
              ),
              TextButton(
                style: destructive
                    ? TextButton.styleFrom(foregroundColor: AppColors.danger)
                    : null,
                onPressed: () => Navigator.pop(dialog, true),
                child: Text(confirmLabel),
              ),
            ],
          ),
        );
  return confirmed ?? false;
}
