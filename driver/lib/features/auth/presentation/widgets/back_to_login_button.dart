import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router.dart';
import '../../../../core/widgets/adaptive/app_button.dart';

class BackToLoginButton extends StatelessWidget {
  const BackToLoginButton({super.key, required this.busy});

  // While a request is running the button is disabled.
  final bool busy;

  @override
  Widget build(BuildContext context) => AppButton(
    label: 'Retour à la connexion',
    variant: AppButtonVariant.secondary,
    onPressed: busy ? null : () => context.go(Routes.login),
  );
}
