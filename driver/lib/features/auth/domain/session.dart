sealed class Session {
  const Session();
}

class SessionLoading extends Session {
  const SessionLoading();
}

class SignedOut extends Session {
  // true when the server rejected the session (e.g. signed in on another device), not a voluntary logout.
  final bool forced;
  const SignedOut({this.forced = false});
}

// Signed in with the password Ndzela set
class PasswordChangeRequired extends Session {
  const PasswordChangeRequired();
}

class SignedIn extends Session {
  const SignedIn();
}
