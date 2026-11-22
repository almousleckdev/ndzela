sealed class Session {
  const Session();
}

class SessionLoading extends Session {
  const SessionLoading();
}

// Why the server ended a session the driver did not end: a sign-in on another phone, or a session
// that can no longer be renewed.
enum SessionEndReason { replaced, expired }

class SignedOut extends Session {
  // Set when the server ended the session, not the driver; null after a voluntary sign-out.
  final SessionEndReason? reason;
  const SignedOut({this.reason});
}

// Signed in with the password Ndzela set
class PasswordChangeRequired extends Session {
  const PasswordChangeRequired();
}

class SignedIn extends Session {
  const SignedIn();
}
