import 'package:flutter/material.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../app/theme/brand_colors.dart';
import '../../../../core/widgets/display/app_avatar.dart';
import '../../../../core/widgets/layout/header_band.dart';
import '../../domain/driver_profile.dart';

// The driver's picture, name and phone
class ProfileHeader extends StatelessWidget {
  const ProfileHeader({super.key, required this.profile, this.photo});

  final DriverProfile? profile;
  final ImageProvider? photo;

  @override
  Widget build(BuildContext context) {
    return HeaderBand(
      bottomPadding: 16,
      child: Row(
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
