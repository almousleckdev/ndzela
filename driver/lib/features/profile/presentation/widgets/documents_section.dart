import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/display/row_divider.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../domain/document_kind.dart';
import '../profile_providers.dart';
import 'contract_file_actions.dart';
import 'document_row.dart';

class DocumentsSection extends ConsumerWidget {
  const DocumentsSection({super.key});

  static const _labels = {
    DocumentKind.contract: 'Contrat de location',
    DocumentKind.licence: 'Permis de conduire',
    DocumentKind.idCard: "Pièce d'identité",
    DocumentKind.addressProof: 'Casier judiciaire',
    DocumentKind.residenceCertificate: 'Certificat de résidence',
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return SectionCard(
      title: 'Mes documents',
      child: AsyncView(
        value: ref.watch(documentsProvider),
        onRetry: () => ref.invalidate(documentsProvider),
        data: (documents) => Column(
          children: [
            for (var i = 0; i < documents.length; i++) ...[
              if (i > 0) const RowDivider(),
              DocumentRow(
                name: _labels[documents[i].kind]!,
                available: documents[i].available,
                actions: documents[i].kind == DocumentKind.contract
                    ? const ContractFileActions()
                    : null,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
