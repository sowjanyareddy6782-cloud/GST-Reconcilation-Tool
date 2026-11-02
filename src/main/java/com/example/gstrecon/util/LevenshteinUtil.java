package com.example.gstrecon.util;

public final class LevenshteinUtil {
    private LevenshteinUtil() { }
    public static int distance(String a, String b) {
        if (a == null || b == null) throw new IllegalArgumentException("Inputs cannot be null");
        if (a.length() < b.length()) return distance(b, a);
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1]; current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }
        return previous[b.length()];
    }
    public static double similarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        int max = Math.max(a.length(), b.length());
        return 1.0 - (double) distance(a, b) / max;
    }
}
