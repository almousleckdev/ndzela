import 'app_exception.dart';

// The French text shown for a failure; clients branch on the server's `code`, never on its message.
String messageFor(Object error) {
  final exception = AppException.from(error);
  return switch (exception.code) {
    'INVALID_CREDENTIALS' => 'Identifiant ou mot de passe incorrect.',
    'ACCOUNT_LOCKED' => 'Compte verrouillé. Réessayez plus tard.',
    'DRIVER_BLOCKED' => 'Votre compte est bloqué. Contactez Ndzela.',
    'PASSWORD_INCORRECT' => 'Mot de passe actuel incorrect.',
    'PASSWORD_UNCHANGED' =>
      'Le nouveau mot de passe doit être différent de l\'actuel.',
    'INVALID_RESET_CODE' => 'Code incorrect ou expiré. Demandez-en un nouveau.',
    'TOO_MANY_MESSAGES' =>
      'Trop de messages envoyés. Réessayez dans une heure.',
    'NOTHING_TO_PAY' => 'Aucun loyer à régulariser.',
    'PAYMENT_IN_PROGRESS' => 'Un paiement est déjà en attente de confirmation.',
    'OPERATOR_NOT_AVAILABLE' => "Cet opérateur n'est pas disponible.",
    'PHONE_INVALID' => 'Numéro de téléphone invalide.',
    'PAYMENT_PROVIDER_UNAVAILABLE' =>
      'Le service de paiement est indisponible. Réessayez dans un instant.',
    'RECEIPT_NOT_AVAILABLE' =>
      "Le reçu n'est disponible que pour un paiement confirmé.",
    'NETWORK_ERROR' => 'Connexion impossible. Vérifiez votre réseau.',
    _ => 'Une erreur est survenue. Réessayez.',
  };
}
