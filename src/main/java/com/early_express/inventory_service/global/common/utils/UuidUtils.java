package com.early_express.inventory_service.global.common.utils;

import java.util.UUID;

/**
 * UUID 생성 유틸리티
 * 표준 36자 UUID만 생성
 * */
public class UuidUtils {
    
    private UuidUtils() {
        throw new AssertionError("유틸리티 클래스는 인스턴스화 할 수 없습니다.");
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static boolean isValid(String uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
