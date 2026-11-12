package com.qinnotek.photo.util;

import java.security.SecureRandom;

/**
 * 제출 링크 토큰 생성. 헷갈리는 글자(0/O, 1/l/I)를 뺀 57자 중 22자 → 약 128비트.
 */
public final class LinkTokens {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LENGTH = 22;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LinkTokens() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
