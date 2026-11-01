package com.ndzela.admin.common.export;

// What sits on top of every export: the brand's name and logo (null: none), the dates the table covers, and when it was exported
public record ExportHeader(String brand, byte[] logo, String period, String exportedAt) {}
