import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/platform/app_icons.dart';
import '../../core/widgets/navigation/app_bottom_bar.dart';
import '../../core/widgets/navigation/app_tab.dart';
import '../../features/payments/domain/driver_access.dart';
import '../../features/payments/presentation/driver_access_provider.dart';

class MainShell extends ConsumerWidget {
  const MainShell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  static const _tabs = [
    AppTab(
      label: 'Accueil',
      icon: AppIcons.home,
      activeIcon: AppIcons.homeActive,
    ),
    AppTab(label: 'Payer', icon: AppIcons.pay, activeIcon: AppIcons.payActive),
    AppTab(
      label: 'Historique',
      icon: AppIcons.history,
      activeIcon: AppIcons.historyActive,
    ),
    AppTab(
      label: 'Profil',
      icon: AppIcons.profile,
      activeIcon: AppIcons.profileActive,
    ),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // A restricted driver sees the payment screen alone: no way to the other tabs.
    final restricted =
        ref.watch(driverAccessProvider) == DriverAccess.restricted;
    return Scaffold(
      body: shell,
      bottomNavigationBar: restricted
          ? null
          : AppBottomBar(
              tabs: _tabs,
              selectedIndex: shell.currentIndex,
              onSelected: (index) => shell.goBranch(
                index,
                initialLocation: index == shell.currentIndex,
              ),
            ),
    );
  }
}
