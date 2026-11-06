enum DocumentKind {
  contract('CONTRACT', null),
  licence('LICENCE', 'licence'),
  idCard('ID', 'id'),
  addressProof('ADDRESS_PROOF', 'address-proof'),
  residenceCertificate('RESIDENCE_CERTIFICATE', 'residence-certificate');

  const DocumentKind(this.code, this.uploadSlug);

  final String code;

  // The name used to send the document to the server; the contract is drawn by the server and never sent.
  final String? uploadSlug;

  bool get canBeAdded => uploadSlug != null;

  static DocumentKind? fromCode(String code) {
    for (final kind in values) {
      if (kind.code == code) return kind;
    }
    return null;
  }
}
