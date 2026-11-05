import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../app/theme/app_theme.dart';
import '../../platform/app_icon.dart';
import 'field_label.dart';
import 'platform_icon.dart';

// The iOS rendering of AppTextField
class CupertinoFormTextField extends StatelessWidget {
  const CupertinoFormTextField({
    super.key,
    required this.label,
    required this.enabled,
    required this.obscureText,
    this.minLines,
    this.maxLines = 1,
    this.maxLength,
    this.controller,
    this.hint,
    this.prefixIcon,
    this.suffix,
    this.keyboardType,
    this.textInputAction,
    this.autofillHints,
    this.inputFormatters,
    this.validator,
    this.onChanged,
    this.onSubmitted,
  });

  final String label;
  final bool enabled;
  final bool obscureText;
  final int? minLines;
  final int maxLines;
  final int? maxLength;
  final TextEditingController? controller;
  final String? hint;
  final AppIcon? prefixIcon;
  final Widget? suffix;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final Iterable<String>? autofillHints;
  final List<TextInputFormatter>? inputFormatters;
  final FormFieldValidator<String>? validator;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    return FormField<String>(
      initialValue: controller?.text ?? '',
      validator: (value) => validator?.call(controller?.text ?? value),
      builder: (state) {
        final hasError = state.hasError;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            FieldLabel(label),
            CupertinoTextField(
              controller: controller,
              enabled: enabled,
              obscureText: obscureText,
              minLines: minLines,
              maxLines: obscureText ? 1 : maxLines,
              maxLength: maxLength,
              keyboardType: keyboardType,
              textInputAction: textInputAction,
              autofillHints: autofillHints,
              inputFormatters: inputFormatters,
              placeholder: hint,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
              style: AppTextStyles.body,
              placeholderStyle: AppTextStyles.body.copyWith(
                color: AppColors.muted,
              ),
              prefix: prefixIcon == null
                  ? null
                  : Padding(
                      padding: const EdgeInsets.only(left: 12),
                      child: PlatformIcon(
                        prefixIcon!,
                        size: 20,
                        color: AppColors.muted,
                      ),
                    ),
              suffix: suffix,
              decoration: BoxDecoration(
                color: CupertinoColors.white,
                borderRadius: BorderRadius.circular(AppTheme.radius),
                border: Border.all(
                  color: hasError ? AppColors.danger : AppColors.line,
                ),
              ),
              onChanged: (value) {
                state.didChange(value);
                onChanged?.call(value);
              },
              onSubmitted: onSubmitted,
            ),
            if (hasError)
              Padding(
                padding: const EdgeInsets.only(top: 6, left: 4),
                child: Text(
                  state.errorText!,
                  style: AppTextStyles.caption.copyWith(
                    color: AppColors.danger,
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}
