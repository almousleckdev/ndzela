enum DocumentKind {
  contract('CONTRACT'),
  licence('LICENCE'),
  idCard('ID'),
  addressProof('ADDRESS_PROOF');

  const DocumentKind(this.code);

  final String code;

  static DocumentKind? fromCode(String code) {
    for (final kind in values) {
      if (kind.code == code) return kind;
    }
    return null;
  }
}
