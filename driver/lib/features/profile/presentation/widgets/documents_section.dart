import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/error_messages.dart';
import '../../../../core/widgets/adaptive/app_message.dart';
import '../../../../core/widgets/display/row_divider.dart';
import '../../../../core/widgets/display/section_card.dart';
import '../../../../core/widgets/feedback/async_view.dart';
import '../../domain/document_kind.dart';
import '../document_upload_controller.dart';
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

  Future<void> _add(
    BuildContext context,
    WidgetRef ref,
    DocumentKind kind,
  ) async {
    try {
      final sent = await ref
          .read(documentUploadControllerProvider.notifier)
          .add(kind);
      if (sent && context.mounted) {
        showAppMessage(context, 'Document ajouté. Merci.');
      }
    } catch (error) {
      if (context.mounted) showAppMessage(context, messageFor(error));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final adding = ref.watch(documentUploadControllerProvider);
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
                adding: adding == documents[i].kind,
                onAdd: documents[i].kind.canBeAdded && adding == null
                    ? () => _add(context, ref, documents[i].kind)
                    : null,
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
