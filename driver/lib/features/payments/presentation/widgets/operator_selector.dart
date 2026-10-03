import 'package:flutter/material.dart';

import '../../domain/payment_operator.dart';
import 'operator_tile.dart';

class OperatorSelector extends StatelessWidget {
  const OperatorSelector({
    super.key,
    required this.operators,
    required this.selected,
    required this.onSelected,
  });

  final List<PaymentOperator> operators;
  final PaymentOperator? selected;
  final ValueChanged<PaymentOperator> onSelected;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (final operator in operators) ...[
          Expanded(
            child: OperatorTile(
              operator: operator,
              selected: operator == selected,
              onTap: () => onSelected(operator),
            ),
          ),
          if (operator != operators.last) const SizedBox(width: 12),
        ],
      ],
    );
  }
}
