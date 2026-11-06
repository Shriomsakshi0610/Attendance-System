package com.renaissance.util;

import java.security.SecureRandom;

/**
 * Generates secure, collision-resistant, human-readable attendance codes.
 */
public final class AttendanceCodeGenerator {

    // Characters chosen to avoid optical confusion (e.g. no 0 vs O, 1 vs I)
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AttendanceCodeGenerator() {
    }

    /**
     * Generates a 6-character random uppercase alphanumeric attendance code.
     */
    public static String generateCode() {
        return generateCode(DEFAULT_CODE_LENGTH);
    }

    /**
     * Generates a code of specified length.
     */
    public static String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(index));
        }
        return sb.toString();
    }
}
