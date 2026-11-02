package com.example.gstrecon.util;

import java.util.Locale;

public final class NormalizerUtil {
    private NormalizerUtil() { }
    public static String normalizeInvoice(String value) {
        if (value == null) return "";
        String clean = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return clean.replaceFirst("^0+(?=[A-Z0-9])", "");
    }
    public static String normalizeGstin(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    public static boolean isValidGstin(String value) {
        return value != null && value.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    }
}
