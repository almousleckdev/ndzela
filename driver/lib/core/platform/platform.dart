import 'package:flutter/material.dart';

// True on Apple platforms, where the app must render Cupertino widgets and never Material ones.
bool isCupertino(BuildContext context) {
  final platform = Theme.of(context).platform;
  return platform == TargetPlatform.iOS || platform == TargetPlatform.macOS;
}
