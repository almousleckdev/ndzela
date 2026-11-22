import 'dart:io';

import 'package:path_provider/path_provider.dart';

import 'token_storage.dart';

// The iOS keychain survives an uninstall: after a fresh install the stored tokens of the previous installation are wiped, so a reinstalled app never opens straight into an old session. It also ends the session of a driver who chose not to be remembered.
class FirstRunGuard {
  const FirstRunGuard(this._tokens);

  static const _marker = '.installed';

  final TokenStorage _tokens;

  Future<void> run() async {
    final directory = await getApplicationSupportDirectory();
    final marker = File('${directory.path}/$_marker');
    if (!await marker.exists()) {
      await _tokens.clearAll();
      await marker.create(recursive: true);
      return;
    }
    // A driver who did not tick "Se souvenir de moi" signs in again after the app was closed.
    if (!await _tokens.readRemember()) await _tokens.clear();
  }
}
