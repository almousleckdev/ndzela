// What the last step of a forgotten password needs: who asked, and the code that was checked.
class ResetCredentials {
  const ResetCredentials({required this.identifier, required this.code});

  final String identifier;
  final String code;
}
