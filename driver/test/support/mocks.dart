import 'package:driver/features/auth/data/auth_repository.dart';
import 'package:driver/features/contract/data/contract_repository.dart';
import 'package:driver/features/payments/data/payments_repository.dart';
import 'package:driver/features/profile/data/profile_repository.dart';
import 'package:driver/features/support/data/support_repository.dart';
import 'package:mocktail/mocktail.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

class MockProfileRepository extends Mock implements ProfileRepository {}

class MockContractRepository extends Mock implements ContractRepository {}

class MockPaymentsRepository extends Mock implements PaymentsRepository {}

class MockSupportRepository extends Mock implements SupportRepository {}
