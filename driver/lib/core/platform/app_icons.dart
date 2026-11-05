import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

import 'app_icon.dart';

abstract final class AppIcons {
  static const home = AppIcon(Icons.home_outlined, CupertinoIcons.house);
  static const homeActive = AppIcon(
    Icons.home_rounded,
    CupertinoIcons.house_fill,
  );
  static const pay = AppIcon(
    Icons.payments_outlined,
    CupertinoIcons.creditcard,
  );
  static const payActive = AppIcon(
    Icons.payments_rounded,
    CupertinoIcons.creditcard_fill,
  );
  static const history = AppIcon(
    Icons.receipt_long_outlined,
    CupertinoIcons.doc_text,
  );
  static const historyActive = AppIcon(
    Icons.receipt_long_rounded,
    CupertinoIcons.doc_text_fill,
  );
  static const profile = AppIcon(
    Icons.person_outline_rounded,
    CupertinoIcons.person,
  );
  static const profileActive = AppIcon(
    Icons.person_rounded,
    CupertinoIcons.person_fill,
  );
  static const notifications = AppIcon(
    Icons.notifications_none_rounded,
    CupertinoIcons.bell,
  );
  static const lock = AppIcon(Icons.lock_outline, CupertinoIcons.lock);
  static const showSecret = AppIcon(
    Icons.visibility_outlined,
    CupertinoIcons.eye,
  );
  static const hideSecret = AppIcon(
    Icons.visibility_off_outlined,
    CupertinoIcons.eye_slash,
  );
  static const error = AppIcon(
    Icons.error_outline,
    CupertinoIcons.exclamationmark_circle,
  );
  static const offline = AppIcon(
    Icons.cloud_off_outlined,
    CupertinoIcons.wifi_slash,
  );
  static const done = AppIcon(
    Icons.check_circle_outline,
    CupertinoIcons.checkmark_circle,
  );
  static const selected = AppIcon(
    Icons.check_circle_rounded,
    CupertinoIcons.checkmark_circle_fill,
  );
  static const unselected = AppIcon(
    Icons.radio_button_unchecked,
    CupertinoIcons.circle,
  );
  static const late = AppIcon(Icons.schedule_rounded, CupertinoIcons.clock);
  static const car = AppIcon(
    Icons.directions_car_filled_rounded,
    CupertinoIcons.car_detailed,
  );
  static const plate = AppIcon(Icons.pin_outlined, CupertinoIcons.number);
  static const phone = AppIcon(Icons.phone_outlined, CupertinoIcons.phone);
  static const email = AppIcon(Icons.mail_outline, CupertinoIcons.mail);
  static const person = AppIcon(Icons.person_outline, CupertinoIcons.person);
  static const mobile = AppIcon(
    Icons.smartphone_rounded,
    CupertinoIcons.device_phone_portrait,
  );
  static const share = AppIcon(Icons.ios_share_rounded, CupertinoIcons.share);
  static const next = AppIcon(
    Icons.chevron_right_rounded,
    CupertinoIcons.chevron_right,
  );
  static const back = AppIcon(Icons.arrow_back_rounded, CupertinoIcons.back);
  static const birthday = AppIcon(Icons.cake_outlined, CupertinoIcons.gift);
  static const address = AppIcon(
    Icons.location_on_outlined,
    CupertinoIcons.location,
  );
  static const contract = AppIcon(
    Icons.description_outlined,
    CupertinoIcons.doc_text,
  );
  static const calendar = AppIcon(
    Icons.event_outlined,
    CupertinoIcons.calendar,
  );
  static const duration = AppIcon(
    Icons.timelapse_rounded,
    CupertinoIcons.timer,
  );
  static const money = AppIcon(
    Icons.payments_outlined,
    CupertinoIcons.money_dollar_circle,
  );
  static const document = AppIcon(
    Icons.insert_drive_file_outlined,
    CupertinoIcons.doc,
  );
  static const call = AppIcon(Icons.call_rounded, CupertinoIcons.phone_fill);
  // WhatsApp's own logo, the same on both platforms.
  static final whatsapp = AppIcon(
    FontAwesomeIcons.whatsapp.data,
    FontAwesomeIcons.whatsapp.data,
  );
  static const chat = AppIcon(
    Icons.chat_bubble_outline_rounded,
    CupertinoIcons.chat_bubble_text,
  );
  static const mail = AppIcon(Icons.mail_rounded, CupertinoIcons.mail_solid);
  static const download = AppIcon(
    Icons.file_download_outlined,
    CupertinoIcons.cloud_download,
  );
  static const open = AppIcon(Icons.open_in_new_rounded, CupertinoIcons.eye);
  static const send = AppIcon(Icons.send_rounded, CupertinoIcons.paperplane);
  static const support = AppIcon(
    Icons.headset_mic_outlined,
    CupertinoIcons.question_circle,
  );
  static const logout = AppIcon(
    Icons.logout,
    CupertinoIcons.square_arrow_right,
  );
}
