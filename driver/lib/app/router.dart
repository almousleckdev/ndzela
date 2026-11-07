import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/widgets/feedback/splash_screen.dart';
import 'splash_hold.dart';
import '../features/auth/domain/session.dart';
import '../features/auth/presentation/change_password_screen.dart';
import '../features/auth/presentation/forgot_password_screen.dart';
import '../features/auth/domain/reset_credentials.dart';
import '../features/auth/presentation/login_screen.dart';
import '../features/auth/presentation/new_password_screen.dart';
import '../features/auth/presentation/verify_code_screen.dart';
import '../features/auth/presentation/session_controller.dart';
import '../features/contract/presentation/home_screen.dart';
import '../features/payments/domain/driver_access.dart';
import '../features/payments/presentation/driver_access_provider.dart';
import '../features/payments/presentation/history_screen.dart';
import '../features/payments/presentation/payment_detail_screen.dart';
import '../features/payments/presentation/payment_screen.dart';
import '../features/profile/presentation/profile_screen.dart';
import '../features/support/presentation/support_message_screen.dart';
import 'shell/main_shell.dart';

abstract final class Routes {
  static const splash = '/';
  static const login = '/login';
  static const changePassword = '/change-password';
  static const forgotPassword = '/forgot-password';
  static const verifyCode = '/forgot-password/code';
  static const newPassword = '/forgot-password/new';
  static const home = '/home';
  static const pay = '/pay';
  static const history = '/history';
  static const profile = '/profile';
  static const supportMessage = '/profile/message';

  static String paymentDetail(String id) => '/history/$id';
}

final _rootNavigatorKey = GlobalKey<NavigatorState>();

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  ref.listen(sessionControllerProvider, (_, _) => refresh.value++);
  ref.listen(driverAccessProvider, (_, _) => refresh.value++);
  ref.listen(splashHeldProvider, (_, _) => refresh.value++);
  ref.onDispose(refresh.dispose);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: Routes.splash,
    refreshListenable: refresh,
    redirect: (context, state) {
      final at = state.matchedLocation;
      if (ref.read(splashHeldProvider)) {
        return at == Routes.splash ? null : Routes.splash;
      }
      return switch (ref.read(sessionControllerProvider)) {
        SessionLoading() => at == Routes.splash ? null : Routes.splash,
        SignedOut() =>
          (at == Routes.login || at.startsWith(Routes.forgotPassword))
              ? null
              : Routes.login,
        PasswordChangeRequired() =>
          at == Routes.changePassword ? null : Routes.changePassword,
        SignedIn() => switch (ref.read(driverAccessProvider)) {
          // Wait for the arrears before showing anything, so a locked driver never sees the home screen.
          DriverAccess.loading => at == Routes.splash ? null : Routes.splash,
          // Only the payment screen is open, and the message to support (a locked driver may need help paying).
          DriverAccess.restricted =>
            (at == Routes.pay || at == Routes.supportMessage)
                ? null
                : Routes.pay,
          DriverAccess.open =>
            (at == Routes.splash ||
                    at == Routes.login ||
                    at == Routes.changePassword)
                ? Routes.home
                : null,
        },
      };
    },
    routes: [
      GoRoute(path: Routes.splash, builder: (_, _) => const SplashScreen()),
      GoRoute(path: Routes.login, builder: (_, _) => const LoginScreen()),
      GoRoute(
        path: Routes.forgotPassword,
        builder: (_, _) => const ForgotPasswordScreen(),
        routes: [
          GoRoute(
            path: 'code',
            builder: (_, state) =>
                VerifyCodeScreen(identifier: state.extra! as String),
          ),
          GoRoute(
            path: 'new',
            builder: (_, state) => NewPasswordScreen(
              credentials: state.extra! as ResetCredentials,
            ),
          ),
        ],
      ),
      GoRoute(
        path: Routes.changePassword,
        builder: (_, _) => const ChangePasswordScreen(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (_, _, shell) => MainShell(shell: shell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(path: Routes.home, builder: (_, _) => const HomeScreen()),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.pay,
                builder: (_, _) => const PaymentScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.history,
                builder: (_, _) => const HistoryScreen(),
                routes: [
                  GoRoute(
                    path: ':id',
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (_, state) =>
                        PaymentDetailScreen(id: state.pathParameters['id']!),
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.profile,
                builder: (_, _) => const ProfileScreen(),
                routes: [
                  GoRoute(
                    path: 'message',
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (_, state) => SupportMessageScreen(
                      initialMessage: state.extra as String?,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );
});
