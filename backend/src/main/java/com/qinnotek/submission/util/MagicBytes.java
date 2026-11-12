package com.qinnotek.submission.util;

import java.util.Locale;
import java.util.Set;

/**
 * 파일 앞부분 시그니처로 확장자 위장을 걸러낸다.
 * (확장자만 믿으면 evil.html 을 evil.png 로 바꿔 올리는 것을 막지 못한다)
 */
public final class MagicBytes {

    private MagicBytes() {
    }

    /** 검사에 필요한 최소 바이트 수. */
    public static final int HEAD_SIZE = 16;

    private static final Set<String> ZIP_CONTAINER = Set.of("docx", "xlsx", "pptx", "hwpx");
    private static final Set<String> OLE2_CONTAINER = Set.of("doc", "xls", "ppt", "hwp");

    /**
     * 확장자에 맞는 시그니처인지 확인한다.
     * 시그니처가 정의되지 않은 확장자는 통과시킨다(화이트리스트가 1차 방어선).
     */
    public static boolean matches(byte[] head, String extension) {
        if (head == null || extension == null) {
            return false;
        }
        String ext = extension.toLowerCase(Locale.ROOT);

        if ("pdf".equals(ext)) {
            return startsWith(head, '%', 'P', 'D', 'F');
        }
        if (ZIP_CONTAINER.contains(ext)) {
            // PK\x03\x04 (일반) / PK\x05\x06 (빈 아카이브) / PK\x07\x08 (분할)
            return head.length >= 4 && head[0] == 0x50 && head[1] == 0x4B
                    && (head[2] == 0x03 || head[2] == 0x05 || head[2] == 0x07);
        }
        if (OLE2_CONTAINER.contains(ext)) {
            // OLE2 Compound File: D0 CF 11 E0 A1 B1 1A E1
            return startsWith(head, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
        }
        return switch (ext) {
            case "jpg", "jpeg" -> startsWith(head, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWith(head, 'G', 'I', 'F', '8');
            case "bmp" -> startsWith(head, 'B', 'M');
            // RIFF....WEBP
            case "webp" -> startsWith(head, 'R', 'I', 'F', 'F') && hasAt(head, 8, 'W', 'E', 'B', 'P');
            // ISO-BMFF: 4바이트 박스 크기 뒤에 'ftyp'
            case "heic", "heif" -> hasAt(head, 4, 'f', 't', 'y', 'p');
            default -> true;
        };
    }

    private static boolean startsWith(byte[] head, int... signature) {
        return hasAt(head, 0, signature);
    }

    private static boolean hasAt(byte[] head, int offset, int... signature) {
        if (head.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((head[offset + i] & 0xFF) != (signature[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
