import '../../platform/app_icon.dart';

class AppTab {
  const AppTab({
    required this.label,
    required this.icon,
    required this.activeIcon,
  });

  final String label;
  final AppIcon icon;
  final AppIcon activeIcon;
}
