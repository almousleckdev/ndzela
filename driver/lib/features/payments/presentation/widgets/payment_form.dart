import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../app/theme/app_text_styles.dart';
import '../../../../core/error/error_messages.dart';
import '../../../../core/platform/app_icons.dart';
import '../../../../core/widgets/adaptive/app_button.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../../../../core/widgets/adaptive/app_text_field.dart';
import '../../../../core/widgets/display/section_title.dart';
import '../../../../core/widgets/layout/dismiss_keyboard.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../../profile/presentation/profile_providers.dart';
import '../../domain/arrears_summary.dart';
import '../../domain/mobile_money_number.dart';
import '../operator_label.dart';
import '../payment_form_controller.dart';
import '../payments_providers.dart';
import 'day_count_selector.dart';
import 'operator_selector.dart';
import 'payment_amount_card.dart';
import 'payment_summary_card.dart';

class PaymentForm extends ConsumerStatefulWidget {
  const PaymentForm({super.key, required this.arrears});

  final ArrearsSummary arrears;

  @override
  ConsumerState<PaymentForm> createState() => _PaymentFormState();
}

class _PaymentFormState extends ConsumerState<PaymentForm> {
  final _phone = TextEditingController();

  @override
  void initState() {
    super.initState();
    // Starts from the driver's own number (loaded here if nothing else did
    ref.listenManual(driverProfileProvider, (_, profile) {
      final own = profile.asData?.value.localPhone;
      if (own == null || _phone.text.isNotEmpty) return;
      _phone.text = own.replaceAll(RegExp(r'\D'), '');
      final number = _phone.text;
      Future.microtask(
        () => ref
            .read(paymentFormControllerProvider.notifier)
            .prefillPhone(number),
      );
    }, fireImmediately: true);
  }

  @override
  void dispose() {
    _phone.dispose();
    super.dispose();
  }

  Future<void> _pay() async {
    try {
      await ref.read(paymentFormControllerProvider.notifier).submit();
    } catch (error) {
      if (mounted) {
        showAppMessage(context, messageFor(error));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final form = ref.watch(paymentFormControllerProvider);
    final controller = ref.read(paymentFormControllerProvider.notifier);
    final operators = ref.watch(operatorsProvider);
    final options = widget.arrears.options;
    // A choice the account no longer offers is dropped, and nothing is quoted or sent for it.
    final stale =
        form.days != null && !options.any((option) => option.days == form.days);
    if (stale) Future.microtask(controller.clearDays);
    final key = stale ? null : form.quoteKey;
    final quote = key == null ? null : ref.watch(paymentQuoteProvider(key));
    final quoted = quote?.asData?.value;
    final days = stale
        ? null
        : form.days ?? (options.isEmpty ? null : options.last.days);
    final missing = _missing(form);
    return DismissKeyboard(
      child: ListView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        padding: const EdgeInsets.all(16),
        children: [
          PaymentAmountCard(
            arrears: widget.arrears,
            selected: options
                .where((option) => option.days == days)
                .firstOrNull,
          ),
          if (options.length > 1) ...[
            const SizedBox(height: 16),
            const SectionTitle('Jours à régler'),
            DayCountSelector(
              options: options,
              selected: days ?? options.last.days,
              onSelected: controller.selectDays,
            ),
          ],
          const SizedBox(height: 16),
          const SectionTitle('Numéro Mobile Money'),
          AppTextField(
            label: 'Numéro',
            controller: _phone,
            prefixIcon: AppIcons.phone,
            keyboardType: TextInputType.phone,
            textInputAction: TextInputAction.done,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(15),
            ],
            enabled: !form.submitting,
            onChanged: controller.changePhone,
            validator: (v) =>
                isValidMobileMoneyNumber(v ?? '') ? null : 'Numéro invalide',
          ),
          const SizedBox(height: 16),
          const SectionTitle('Opérateur'),
          AsyncView(
            value: operators,
            onRetry: () => ref.invalidate(operatorsProvider),
            data: (list) => OperatorSelector(
              operators: list,
              selected: form.operator,
              onSelected: controller.selectOperator,
            ),
          ),
          const SizedBox(height: 16),
          const SectionTitle('Récapitulatif'),
          PaymentSummaryCard(
            arrears: widget.arrears,
            form: form,
            quote: quote,
            operatorName: form.operator == null
                ? null
                : operatorLabel(form.operator!.code, operators.asData?.value),
            onRetry: key == null
                ? null
                : () => ref.invalidate(paymentQuoteProvider(key)),
          ),
          const SizedBox(height: 16),
          if (missing != null) ...[
            Text(missing, style: AppTextStyles.caption),
            const SizedBox(height: 8),
          ],
          AppButton(
            label: quoted == null ? 'Payer' : 'Payer ${quoted.total}',
            loading: form.submitting,
            onPressed: form.canSubmit && quoted != null ? _pay : null,
          ),
        ],
      ),
    );
  }

  // What the driver still has to give before paying, in the order the form asks for it
  String? _missing(PaymentFormState form) {
    if (form.operator == null) return 'Choisissez un opérateur pour continuer.';
    if (!isValidMobileMoneyNumber(form.phone)) {
      return 'Saisissez un numéro valide (8 à 15 chiffres).';
    }
    return null;
  }
}
