import 'package:flutter/material.dart';

import '../../platform/app_icons.dart';
import 'app_icon_button.dart';
import 'app_text_field.dart';

// A password field with its own show/hide toggle.
class AppPasswordField extends StatefulWidget {
  const AppPasswordField({
    super.key,
    required this.label,
    this.controller,
    this.textInputAction,
    this.validator,
    this.onSubmitted,
    this.hint = _defaultHint,
    this.enabled = true,
  });

  static const _defaultHint = '••••••••';

  final String label;
  final TextEditingController? controller;
  final TextInputAction? textInputAction;
  final FormFieldValidator<String>? validator;
  final ValueChanged<String>? onSubmitted;
  final String hint;
  final bool enabled;

  @override
  State<AppPasswordField> createState() => _AppPasswordFieldState();
}

class _AppPasswordFieldState extends State<AppPasswordField> {
  bool _hidden = true;

  @override
  Widget build(BuildContext context) {
    return AppTextField(
      label: widget.label,
      hint: widget.hint,
      controller: widget.controller,
      prefixIcon: AppIcons.lock,
      obscureText: _hidden,
      textInputAction: widget.textInputAction,
      autofillHints: const [AutofillHints.password],
      enabled: widget.enabled,
      validator: widget.validator,
      onSubmitted: widget.onSubmitted,
      suffix: AppIconButton(
        icon: _hidden ? AppIcons.showSecret : AppIcons.hideSecret,
        tooltip: _hidden ? 'Afficher' : 'Masquer',
        outlined: false,
        onPressed: () => setState(() => _hidden = !_hidden),
      ),
    );
  }
}
