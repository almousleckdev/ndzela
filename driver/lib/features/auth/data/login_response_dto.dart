class LoginResponseDto {
  const LoginResponseDto({
    required this.accessToken,
    required this.refreshToken,
    required this.mustChangePassword,
  });

  factory LoginResponseDto.fromJson(Map<String, dynamic> json) =>
      LoginResponseDto(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        mustChangePassword: json['mustChangePassword'] as bool? ?? false,
      );

  final String accessToken;
  final String refreshToken;
  final bool mustChangePassword;
}
