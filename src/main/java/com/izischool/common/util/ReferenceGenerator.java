package com.izischool.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ReferenceGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ReferenceGenerator() {
    }

    public static String generateStudentNumber(String schoolCode, long sequence) {
        return String.format("%s-STD-%05d", schoolCode.toUpperCase(), sequence);
    }

    public static String generatePaymentReference(String schoolCode) {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.format("%s-PAY-%s-%s", schoolCode.toUpperCase(), datePart, randomPart);
    }

    public static String generateReceiptNumber(String schoolCode, long sequence) {
        int year = LocalDate.now().getYear();
        return String.format("%s-REC-%d-%06d", schoolCode.toUpperCase(), year, sequence);
    }
}
