import '../../../../app/theme/brand_colors.dart';
import 'package:flutter/material.dart';

import '../../../../app/theme/app_colors.dart';
import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/widgets/display/app_avatar.dart';
import '../../../../core/widgets/layout/header_band.dart';
import '../../domain/driver_profile.dart';

// The profile band
class ProfileHeader extends StatelessWidget {
  const ProfileHeader({super.key, required this.profile, this.photo});

  final DriverProfile? profile;
  final ImageProvider? photo;

  @override
  Widget build(BuildContext context) {
    return HeaderBand(
      bottomPadding: 20,
      child: SizedBox(
        width: double.infinity,
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(3),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: context.brand.primary,
              ),
              child: AppAvatar(
                initials: profile?.initials ?? '',
                photo: photo,
                radius: 38,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              profile?.fullName ?? ' ',
              textAlign: TextAlign.center,
              style: AppTextStyles.display(24),
            ),
            const SizedBox(height: 4),
            Text(
              profile?.phone ?? ' ',
              style: const TextStyle(color: AppColors.muted, fontSize: 15),
            ),
          ],
        ),
      ),
    );
  }
}
