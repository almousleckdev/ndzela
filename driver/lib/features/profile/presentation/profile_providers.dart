import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../data/profile_repository.dart';
import '../domain/driver_document.dart';
import '../domain/driver_profile.dart';

final profileRepositoryProvider = Provider<ProfileRepository>(
  (ref) => ProfileRepository(ref.watch(dioProvider)),
);

final driverProfileProvider = FutureProvider<DriverProfile>(
  (ref) => ref.watch(profileRepositoryProvider).fetchMe(),
);

final documentsProvider = FutureProvider<List<DriverDocument>>(
  (ref) => ref.watch(profileRepositoryProvider).fetchDocuments(),
);

final driverPhotoProvider = FutureProvider<Uint8List?>(
  (ref) => ref.watch(profileRepositoryProvider).fetchPhoto(),
);
