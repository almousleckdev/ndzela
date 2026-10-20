# NdZela driver app

Flutter + Riverpod app for drivers (iOS and Android). Architecture: [docs/mobile/architecture.md](../docs/mobile/architecture.md). Test builds and store release: [docs/mobile/release.md](../docs/mobile/release.md).

| Task | Command |
| --- | --- |
| Run against the local backend | `flutter run` (the Android emulator reaches it at `10.0.2.2:8080`, the iOS simulator at `localhost:8080`) |
| Run against a real backend | `flutter run --dart-define-from-file=env/staging.json` |
| Check | `flutter analyze && flutter test` |
| Regenerate the launcher icons (after changing `assets/icon/app_icon.png`) | `dart run flutter_launcher_icons`, then keep `ASSETCATALOG_COMPILER_GENERATE_SWIFT_ASSET_SYMBOL_EXTENSIONS = YES` in `ios/Runner.xcodeproj/project.pbxproj` (the tool rewrites it wrongly) |
