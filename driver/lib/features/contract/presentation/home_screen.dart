import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/router.dart';
import '../../../core/widgets/adaptive/refreshable_list.dart';
import '../../../core/widgets/feedback/async_view.dart';
import '../../../core/widgets/layout/header_band.dart';
import '../../payments/presentation/payments_providers.dart';
import '../../profile/presentation/profile_providers.dart';
import 'contract_providers.dart';
import 'widgets/balance_card.dart';
import 'widgets/contract_summary_card.dart';
import 'widgets/home_greeting.dart';
import 'widgets/vehicle_card.dart';
import 'widgets/weekly_history_card.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  Future<void> _refresh(WidgetRef ref) async {
    ref
      ..invalidate(driverProfileProvider)
      ..invalidate(driverPhotoProvider)
      ..invalidate(vehicleProvider)
      ..invalidate(contractDetailsProvider)
      ..invalidate(paymentHistoryProvider)
      ..invalidate(arrearsProvider);
    await ref.read(arrearsProvider.future);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contract = ref.watch(contractDetailsProvider).asData?.value;
    return Scaffold(
      body: Column(
        children: [
          const HeaderBand(child: HomeGreeting()),
          Expanded(
            child: RefreshableList(
              onRefresh: () => _refresh(ref),
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
              children: [
                AsyncView(
                  value: ref.watch(arrearsProvider),
                  onRetry: () => ref.invalidate(arrearsProvider),
                  data: (arrears) => BalanceCard(
                    arrears: arrears,
                    onRegularise: () => context.go(Routes.pay),
                  ),
                ),
                const SizedBox(height: 12),
                AsyncView(
                  value: ref.watch(vehicleProvider),
                  onRetry: () => ref.invalidate(vehicleProvider),
                  data: (vehicle) => VehicleCard(vehicle: vehicle),
                ),
                if (contract != null) ...[
                  const SizedBox(height: 12),
                  ContractSummaryCard(contract: contract),
                ],
                const SizedBox(height: 12),
                const WeeklyHistoryCard(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
