import 'package:flutter/material.dart';

import 'app_toast.dart';

export 'app_toast.dart' show AppToastKind;

OverlayEntry? _showing;

// A short, non-blocking message; a new one replaces the one on screen.
void showAppMessage(
  BuildContext context,
  String message, {
  AppToastKind kind = AppToastKind.info,
  String? title,
}) {
  _showing?.remove();
  late final OverlayEntry entry;
  entry = OverlayEntry(
    builder: (overlay) => DefaultTextStyle(
      style: Theme.of(
        overlay,
      ).textTheme.bodyMedium!.copyWith(decoration: TextDecoration.none),
      child: AppToast(
        message: message,
        title: title,
        kind: kind,
        onClosed: () {
          entry.remove();
          if (_showing == entry) _showing = null;
        },
      ),
    ),
  );
  _showing = entry;
  Overlay.of(context, rootOverlay: true).insert(entry);
}
