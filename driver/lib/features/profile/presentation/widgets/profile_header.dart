import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/error/error_messages.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/activity_indicator.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../../../../core/widgets/adaptive/platform_icon.dart';
import '../../../../core/widgets/display/app_avatar.dart';
import '../../../../core/widgets/layout/header_band.dart';
import '../../domain/driver_profile.dart';
import '../photo_upload_controller.dart';

// The driver's picture (a tap changes it), name and phone
class ProfileHeader extends ConsumerWidget {
  const ProfileHeader({super.key, required this.profile, this.photo});

  final DriverProfile? profile;
  final ImageProvider? photo;

  Future<void> _change(BuildContext context, WidgetRef ref) async {
    try {
      final sent = await ref
          .read(photoUploadControllerProvider.notifier)
          .change();
      if (sent && context.mounted) {
        showAppMessage(
          context,
          'Votre photo de profil est à jour.',
          kind: AppToastKind.success,
        );
      }
    } catch (error) {
      if (context.mounted) {
        showAppMessage(context, messageFor(error), kind: AppToastKind.error);
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final uploading = ref.watch(photoUploadControllerProvider);
    return HeaderBand(
      bottomPadding: 16,
      child: Row(
        children: [
          Semantics(
            button: true,
            label: 'Changer ma photo de profil',
            child: GestureDetector(
              onTap: uploading ? null : () => _change(context, ref),
              child: SizedBox(
                width: 60,
                height: 60,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(2),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: context.brand.primary,
                      ),
                      child: AppAvatar(
                        initials: profile?.initials ?? '',
                        photo: photo,
                        radius: 28,
                      ),
                    ),
                    Positioned(
                      right: -2,
                      bottom: -2,
                      child: Container(
                        width: 24,
                        height: 24,
                        decoration: BoxDecoration(
                          color: AppColors.canvas,
                          shape: BoxShape.circle,
                          border: Border.all(color: AppColors.line),
                        ),
                        child: Center(
                          child: uploading
                              ? const AppActivityIndicator(size: 12)
                              : const PlatformIcon(
                                  AppIcons.camera,
                                  size: 14,
                                  color: AppColors.ink,
                                ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  profile?.fullName ?? ' ',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.title,
                ),
                Text(profile?.phone ?? ' ', style: AppTextStyles.label),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
