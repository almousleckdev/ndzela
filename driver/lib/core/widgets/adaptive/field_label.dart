import 'package:flutter/widgets.dart';

import '../../../app/theme/app_text_styles.dart';

// The small label above an input, the same on both platforms.
class FieldLabel extends StatelessWidget {
  const FieldLabel(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 6),
    child: Text(text, style: AppTextStyles.label),
  );
}
