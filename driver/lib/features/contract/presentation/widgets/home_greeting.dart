import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router.dart';
import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_icon_button.dart';
import '../../../../core/widgets/display/app_avatar.dart';
import '../../../profile/presentation/profile_providers.dart';

// "Bonjour," and the driver's name on the left; notifications and the avatar on the right.
class HomeGreeting extends ConsumerWidget {
  const HomeGreeting({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(driverProfileProvider).asData?.value;
    final photo = ref.watch(driverPhotoProvider).asData?.value;
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Bonjour,',
                style: TextStyle(
                  fontSize: 14,
                  color: AppColors.muted,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                profile?.fullName ?? ' ',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTextStyles.display(24),
              ),
            ],
          ),
        ),
        AppIconButton(
          icon: AppIcons.notifications,
          tooltip: 'Notifications',
          onPressed: () {},
        ),
        const SizedBox(width: 10),
        GestureDetector(
          onTap: () => context.go(Routes.profile),
          child: Semantics(
            button: true,
            label: 'Profil',
            child: Container(
              padding: const EdgeInsets.all(2),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: context.brand.primary,
              ),
              child: AppAvatar(
                initials: profile?.initials ?? '',
                photo: photo == null ? null : MemoryImage(photo),
                radius: 21,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
