import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:driver/core/network/auth_interceptor.dart';
import 'package:driver/core/network/token_refresher.dart';
import 'package:driver/core/storage/token_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/memory_storage.dart';

typedef Answer = FutureOr<ResponseBody> Function(RequestOptions options);

class StubAdapter implements HttpClientAdapter {
  StubAdapter(this.answer);

  final Answer answer;
  final calls = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    calls.add(options);
    return answer(options);
  }

  @override
  void close({bool force = false}) {}

  int callsTo(String path) => calls.where((c) => c.path == path).length;
}

ResponseBody json(int status, Map<String, dynamic> body) =>
    ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );

ResponseBody networkDown(RequestOptions options) =>
    throw DioException.connectionError(
      requestOptions: options,
      reason: 'offline',
    );

class Rig {
  Rig(Answer answer, {Map<String, String>? stored})
    : adapter = StubAdapter(answer),
      storage = MemoryStorage(
        stored ??
            {'access_token': 'old-access', 'refresh_token': 'old-refresh'},
      ) {
    tokens = TokenStorage(storage);
    final plain = Dio(BaseOptions(baseUrl: 'https://api.test'))
      ..httpClientAdapter = adapter;
    dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
      ..httpClientAdapter = adapter;
    dio.interceptors.add(
      AuthInterceptor(
        tokens: tokens,
        refresher: TokenRefresher(tokens: tokens, client: plain),
        client: dio,
        onUnauthorized: () => signedOut++,
      ),
    );
  }

  final StubAdapter adapter;
  final MemoryStorage storage;
  late final TokenStorage tokens;
  late final Dio dio;
  int signedOut = 0;
}

ResponseBody renewing(RequestOptions options) {
  if (options.path == '/auth/refresh') {
    return json(200, {
      'accessToken': 'new-access',
      'refreshToken': 'new-refresh',
      'mustChangePassword': false,
    });
  }
  final bearer = options.headers['Authorization'];
  return bearer == 'Bearer new-access'
      ? json(200, {'ok': true})
      : json(401, {'code': 'UNAUTHORIZED'});
}

void main() {
  test(
    'an expired access token is renewed and the request is retried',
    () async {
      final rig = Rig(renewing);

      final response = await rig.dio.get<Map<String, dynamic>>('/account');

      expect(response.data, {'ok': true});
      expect(rig.adapter.callsTo('/auth/refresh'), 1);
      expect(await rig.tokens.readAccess(), 'new-access');
      expect(await rig.tokens.readRefresh(), 'new-refresh');
      expect(rig.signedOut, 0);
    },
  );

  test('requests failing together share one renewal', () async {
    final rig = Rig(renewing);

    final results = await Future.wait([
      rig.dio.get<Map<String, dynamic>>('/account'),
      rig.dio.get<Map<String, dynamic>>('/me'),
      rig.dio.get<Map<String, dynamic>>('/contract'),
      rig.dio.get<Map<String, dynamic>>('/payments'),
    ]);

    expect(results.every((r) => r.statusCode == 200), isTrue);
    expect(rig.adapter.callsTo('/auth/refresh'), 1);
    expect(rig.signedOut, 0);
  });

  test(
    'a request that started before a renewal is retried with the new token, not renewed again',
    () async {
      final release = Completer<void>();
      final rig = Rig((options) async {
        if (options.path == '/auth/refresh') {
          return json(200, {
            'accessToken': 'new-access',
            'refreshToken': 'new-refresh',
            'mustChangePassword': false,
          });
        }
        final fresh = options.headers['Authorization'] == 'Bearer new-access';
        if (options.path == '/slow' && !fresh) await release.future;
        return fresh
            ? json(200, {'ok': true})
            : json(401, {'code': 'UNAUTHORIZED'});
      });

      final slow = rig.dio.get<Map<String, dynamic>>('/slow');
      await Future<void>.delayed(Duration.zero);
      await rig.dio.get<Map<String, dynamic>>('/account');
      release.complete();
      final late = await slow;

      expect(late.statusCode, 200);
      expect(rig.adapter.callsTo('/auth/refresh'), 1);
      expect(rig.signedOut, 0);
    },
  );

  test('a refresh the server rejects signs the driver out', () async {
    final rig = Rig(
      (options) => options.path == '/auth/refresh'
          ? json(401, {'code': 'SESSION_EXPIRED'})
          : json(401, {'code': 'UNAUTHORIZED'}),
    );

    await expectLater(
      rig.dio.get<Map<String, dynamic>>('/account'),
      throwsA(isA<DioException>()),
    );

    expect(rig.signedOut, 1);
  });

  test(
    'losing the network while renewing keeps the driver signed in',
    () async {
      final rig = Rig(
        (options) => options.path == '/auth/refresh'
            ? networkDown(options)
            : json(401, {'code': 'UNAUTHORIZED'}),
      );

      await expectLater(
        rig.dio.get<Map<String, dynamic>>('/account'),
        throwsA(isA<DioException>()),
      );

      expect(rig.signedOut, 0);
      expect(await rig.tokens.readRefresh(), 'old-refresh');
    },
  );

  test('a wrong password at login is not a session problem', () async {
    final rig = Rig((_) => json(401, {'code': 'INVALID_CREDENTIALS'}));

    await expectLater(
      rig.dio.post<Map<String, dynamic>>('/auth/login', data: {}),
      throwsA(isA<DioException>()),
    );

    expect(rig.adapter.callsTo('/auth/refresh'), 0);
    expect(rig.signedOut, 0);
  });

  test(
    'a request that is still refused after a renewal signs the driver out',
    () async {
      final rig = Rig(
        (options) => options.path == '/auth/refresh'
            ? json(200, {
                'accessToken': 'new-access',
                'refreshToken': 'new-refresh',
                'mustChangePassword': false,
              })
            : json(401, {'code': 'SESSION_REPLACED'}),
      );

      await expectLater(
        rig.dio.get<Map<String, dynamic>>('/account'),
        throwsA(isA<DioException>()),
      );

      expect(rig.adapter.callsTo('/auth/refresh'), 1);
      expect(rig.signedOut, 1);
    },
  );
}
