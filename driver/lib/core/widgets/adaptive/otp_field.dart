import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../app/theme/app_colors.dart';
import '../../../app/theme/app_text_styles.dart';
import '../../../app/theme/brand_colors.dart';

// A one-time code typed into separate boxes; one hidden input carries the digits.
class OtpField extends StatefulWidget {
  const OtpField({
    super.key,
    required this.controller,
    this.length = 6,
    this.enabled = true,
    this.onCompleted,
  });

  final TextEditingController controller;
  final int length;
  final bool enabled;
  final ValueChanged<String>? onCompleted;

  @override
  State<OtpField> createState() => _OtpFieldState();
}

class _OtpFieldState extends State<OtpField> {
  final _focus = FocusNode();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_changed);
    _focus.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    widget.controller.removeListener(_changed);
    _focus.dispose();
    super.dispose();
  }

  void _changed() {
    setState(() {});
    final code = widget.controller.text;
    if (code.length == widget.length) widget.onCompleted?.call(code);
  }

  @override
  Widget build(BuildContext context) {
    final code = widget.controller.text;
    return Semantics(
      textField: true,
      label: 'Code à ${widget.length} chiffres',
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: widget.enabled ? _focus.requestFocus : null,
        child: Stack(
          children: [
            Row(
              children: [
                for (var i = 0; i < widget.length; i++) ...[
                  if (i > 0) const SizedBox(width: 8),
                  Expanded(
                    child: _Box(
                      digit: i < code.length ? code[i] : '',
                      active: _focus.hasFocus && i == code.length,
                    ),
                  ),
                ],
              ],
            ),
            // The real input sits under the boxes, invisible.
            Positioned.fill(
              child: Opacity(
                opacity: 0,
                child: EditableText(
                  controller: widget.controller,
                  focusNode: _focus,
                  readOnly: !widget.enabled,
                  autofocus: true,
                  keyboardType: TextInputType.number,
                  autofillHints: const [AutofillHints.oneTimeCode],
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(widget.length),
                  ],
                  style: AppTextStyles.body,
                  cursorColor: Colors.transparent,
                  backgroundCursorColor: Colors.transparent,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Box extends StatelessWidget {
  const _Box({required this.digit, required this.active});

  final String digit;
  final bool active;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: active ? context.brand.primary : AppColors.line,
          width: active ? 1.6 : 1,
        ),
      ),
      child: Text(digit, style: AppTextStyles.figure),
    );
  }
}
