package com.erp.admin.platform.credential;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 凭证掩码工具
 */
public final class CredentialMaskUtil {

    private CredentialMaskUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 对凭证 map 中的值做掩码处理
     */
    public static Map<String, String> mask(Map<String, String> credential) {
        if (credential == null) {
            return Collections.emptyMap();
        }
        Map<String, String> masked = new LinkedHashMap<>();
        credential.forEach((k, v) -> {
            if (!StringUtils.hasText(v)) {
                masked.put(k, "");
            }
            else if (v.length() <= 6) {
                masked.put(k, "***");
            }
            else {
                masked.put(k, v.substring(0, 3) + "***" + v.substring(v.length() - 3));
            }
        });
        return masked;
    }
}