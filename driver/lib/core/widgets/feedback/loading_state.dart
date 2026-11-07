import 'package:flutter_riverpod/flutter_riverpod.dart';

// True while any of the values is loading or loading again, so a screen can show its whole skeleton at once.
bool anyLoading(Iterable<AsyncValue<Object?>> values) =>
    values.any((value) => value.isLoading);
