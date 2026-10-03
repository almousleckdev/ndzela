sealed class Session {
  const Session();
}

class SessionLoading extends Session {
  const SessionLoading();
}

class SignedOut extends Session {
  const SignedOut();
}

// Signed in with the password Ndzela set
class PasswordChangeRequired extends Session {
  const PasswordChangeRequired();
}

class SignedIn extends Session {
  const SignedIn();
}
