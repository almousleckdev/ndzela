import 'dart:async';

import 'package:flutter/material.dart';

import '../../platform/platform.dart';
import 'toast_banner.dart';

// A short, non-blocking message
void showAppMessage(BuildContext context, String message) {
  if (!isCupertino(context)) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
    return;
  }
  final entry = OverlayEntry(builder: (_) => ToastBanner(message: message));
  Overlay.of(context, rootOverlay: true).insert(entry);
  Timer(const Duration(seconds: 3), entry.remove);
}
