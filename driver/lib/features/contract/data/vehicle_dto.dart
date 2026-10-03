import '../../../core/network/media_url.dart';
import '../domain/vehicle_info.dart';

class VehicleDto {
  const VehicleDto({
    required this.brand,
    required this.model,
    required this.plate,
    this.photo,
  });

  factory VehicleDto.fromJson(Map<String, dynamic> json) => VehicleDto(
    brand: json['brand'] as String,
    model: json['model'] as String,
    plate: json['plate'] as String,
    photo: json['photoUrl'] as String?,
  );

  final String brand;
  final String model;
  final String plate;
  final String? photo;

  VehicleInfo toDomain() => VehicleInfo(
    name: '$brand $model',
    plate: plate,
    imageUrl: mediaUrl(photo),
  );
}
