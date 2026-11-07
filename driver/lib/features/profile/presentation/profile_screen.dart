import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/widgets/feedback/async_view.dart';
import '../../../core/widgets/feedback/loading_state.dart';
import '../../../core/widgets/feedback/screen_skeleton.dart';
import '../../contract/presentation/contract_providers.dart';
import 'profile_providers.dart';
import 'widgets/contract_section.dart';
import 'widgets/documents_section.dart';
import 'widgets/logout_button.dart';
import 'widgets/personal_info_section.dart';
import 'widgets/privacy_section.dart';
import 'widgets/profile_stats.dart';
import 'widgets/profile_header.dart';
import 'widgets/support_section.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(driverProfileProvider);
    final loading = anyLoading([
      profile,
      ref.watch(contractDetailsProvider),
      ref.watch(documentsProvider),
    ]);
    final photo = ref.watch(driverPhotoProvider).asData?.value;
    return Scaffold(
      body: Column(
        children: [
          ProfileHeader(
            profile: profile.asData?.value,
            photo: photo == null ? null : MemoryImage(photo),
          ),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
              children: loading
                  ? const [ScreenSkeleton()]
                  : [
                      const ProfileStats(),
                      const SizedBox(height: 16),
                      AsyncView(
                        value: profile,
                        onRetry: () => ref.invalidate(driverProfileProvider),
                        data: (me) => PersonalInfoSection(profile: me),
                      ),
                      const SizedBox(height: 16),
                      const ContractSection(),
                      const SizedBox(height: 16),
                      const DocumentsSection(),
                      const SizedBox(height: 16),
                      const SupportSection(),
                      const SizedBox(height: 16),
                      const PrivacySection(),
                      const SizedBox(height: 20),
                      const LogoutButton(),
                      const SizedBox(height: 12),
                    ],
            ),
          ),
        ],
      ),
    );
  }
}
