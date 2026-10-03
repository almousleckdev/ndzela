import 'package:url_launcher/url_launcher.dart';

// Opens a phone call, a chat or a mail composer outside the app; false when nothing can handle it.
Future<bool> openExternal(Uri uri) async {
  try {
    return await launchUrl(uri, mode: LaunchMode.externalApplication);
  } on Exception {
    return false;
  }
}

Uri phoneUri(String number) => Uri(scheme: 'tel', path: number);

Uri whatsappUri(String number) =>
    Uri.https('wa.me', '/${number.replaceAll(RegExp(r'\D'), '')}');

Uri emailUri(String address) => Uri(scheme: 'mailto', path: address);
