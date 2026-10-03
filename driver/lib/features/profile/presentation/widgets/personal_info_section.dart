import 'package:flutter/material.dart';

import '../../../../core/formatting/date_format.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/display/info_row.dart';
import '../../../../core/widgets/display/row_divider.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../domain/driver_profile.dart';

class PersonalInfoSection extends StatelessWidget {
  const PersonalInfoSection({super.key, required this.profile});

  final DriverProfile profile;

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[
      InfoRow(icon: AppIcons.phone, label: 'Téléphone', value: profile.phone),
      if (profile.email != null)
        InfoRow(icon: AppIcons.email, label: 'E-mail', value: profile.email!),
      if (profile.birthDate != null)
        InfoRow(
          icon: AppIcons.birthday,
          label: 'Date de naissance',
          value: formatDate(profile.birthDate!),
        ),
      if (profile.address != null)
        InfoRow(
          icon: AppIcons.address,
          label: 'Adresse',
          value: profile.address!,
        ),
    ];
    return SectionCard(
      title: 'Informations personnelles',
      child: Column(
        children: [
          for (var i = 0; i < rows.length; i++) ...[
            if (i > 0) const RowDivider(),
            rows[i],
          ],
        ],
      ),
    );
  }
}
