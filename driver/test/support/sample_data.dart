import 'dart:typed_data';

import 'package:driver/core/money/money.dart';
import 'package:driver/features/contract/domain/contract_details.dart';
import 'package:driver/features/contract/domain/contract_progress.dart';
import 'package:driver/features/contract/domain/vehicle_info.dart';
import 'package:driver/features/payments/domain/payment_operator.dart';
import 'package:driver/features/payments/domain/payment_quote.dart';
import 'package:driver/features/profile/domain/document_kind.dart';
import 'package:driver/features/profile/domain/driver_document.dart';
import 'package:driver/features/profile/domain/driver_profile.dart';
import 'package:driver/features/support/domain/support_contacts.dart';

// What the fake backend answers by default: one driver, one contract, one car.
final sampleProfile = DriverProfile(
  firstName: 'Awa',
  lastName: 'Nze',
  phone: '+24174111111',
  localPhone: '074111111',
  email: 'awa@ndzela.test',
  birthDate: DateTime(1990, 5, 12),
  address: 'Rue 12, Akanda, Libreville',
);

const sampleDocuments = [
  DriverDocument(kind: DocumentKind.contract, available: true),
  DriverDocument(kind: DocumentKind.licence, available: true),
  DriverDocument(kind: DocumentKind.idCard, available: true),
  DriverDocument(kind: DocumentKind.addressProof, available: false),
];

final sampleContract = ContractDetails(
  number: 'NDZ-77836447',
  startDate: DateTime(2026, 10, 1),
  endDate: DateTime(2029, 9, 30),
  durationMonths: 36,
  dailyRent: const Money(23000, 'XAF'),
  progress: const ContractProgress(
    totalDays: 1248,
    elapsedDays: 12,
    daysLeft: 1236,
    paidDays: 10,
    unpaidDays: 2,
    totalPrice: Money(28704000, 'XAF'),
    paidAmount: Money(230000, 'XAF'),
    remainingAmount: Money(28474000, 'XAF'),
    deposit: Money(500000, 'XAF'),
  ),
);

const sampleSupport = SupportContacts(
  phone: '+24162821855',
  whatsapp: '+24162821855',
  email: 'ndzelagabon@gmail.com',
);

const sampleVehicle = VehicleInfo(name: 'Toyota Yaris', plate: 'AB-123-CD');

const sampleOperators = [
  PaymentOperator(code: 'AIRTEL_MONEY', label: 'Airtel Money'),
  PaymentOperator(code: 'MOOV_MONEY', label: 'Moov Money'),
];

const sampleQuote = PaymentQuote(
  amount: Money(69000, 'XAF'),
  fee: Money(2415, 'XAF'),
  accountHolder: 'Awa Nze',
);

// The first bytes of a PDF: enough for the share sheet tests.
final sampleReceipt = Uint8List.fromList([0x25, 0x50, 0x44, 0x46]);
