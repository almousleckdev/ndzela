import 'package:driver/core/widgets/feedback/async_view.dart';
import 'package:driver/core/widgets/feedback/screen_skeleton.dart';
import 'package:driver/core/widgets/feedback/shimmer.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _view(AsyncValue<int> value) => MaterialApp(
  home: Scaffold(
    body: AsyncView<int>(
      value: value,
      onRetry: () {},
      skeleton: const ScreenSkeleton(),
      data: (n) => Text('data $n'),
    ),
  ),
);

void main() {
  testWidgets('a first load shows the shimmer', (tester) async {
    await tester.pumpWidget(_view(const AsyncLoading<int>()));

    expect(find.byType(Shimmer), findsOneWidget);
    expect(find.text('data 1'), findsNothing);
  });

  testWidgets('loading again over old data shows the shimmer', (tester) async {
    final provider = FutureProvider<int>((ref) async => 1);
    late WidgetRef captured;
    await tester.pumpWidget(
      ProviderScope(
        child: Consumer(
          builder: (context, ref, _) {
            captured = ref;
            return _view(ref.watch(provider));
          },
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('data 1'), findsOneWidget);

    captured.invalidate(provider);
    await tester.pump();

    expect(find.byType(Shimmer), findsOneWidget);
    expect(find.text('data 1'), findsNothing);
    await tester.pumpAndSettle(const Duration(seconds: 1));
  });

  testWidgets('loaded data shows no shimmer', (tester) async {
    await tester.pumpWidget(_view(const AsyncData(1)));

    expect(find.text('data 1'), findsOneWidget);
    expect(find.byType(Shimmer), findsNothing);
  });
}
