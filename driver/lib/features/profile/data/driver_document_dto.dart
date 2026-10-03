import '../domain/document_kind.dart';
import '../domain/driver_document.dart';

class DriverDocumentDto {
  const DriverDocumentDto({required this.kind, required this.available});

  factory DriverDocumentDto.fromJson(Map<String, dynamic> json) =>
      DriverDocumentDto(
        kind: json['kind'] as String,
        available: json['available'] as bool,
      );

  final String kind;
  final bool available;

  // Null for a kind this app version does not know.
  DriverDocument? toDomain() {
    final parsed = DocumentKind.fromCode(kind);
    return parsed == null
        ? null
        : DriverDocument(kind: parsed, available: available);
  }
}
