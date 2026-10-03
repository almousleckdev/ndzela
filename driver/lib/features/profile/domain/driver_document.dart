import 'document_kind.dart';

class DriverDocument {
  const DriverDocument({required this.kind, required this.available});

  final DocumentKind kind;
  final bool available;
}
