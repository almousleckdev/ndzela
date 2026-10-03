import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../data/support_repository.dart';
import '../domain/support_contacts.dart';

final supportRepositoryProvider = Provider<SupportRepository>(
  (ref) => SupportRepository(ref.watch(dioProvider)),
);

final supportContactsProvider = FutureProvider<SupportContacts>(
  (ref) => ref.watch(supportRepositoryProvider).fetchContacts(),
);
