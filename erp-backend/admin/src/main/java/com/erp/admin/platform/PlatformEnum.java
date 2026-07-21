package com.erp.admin.platform;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PlatformEnum {

	Wildberries("wildberries"),
	Ozon("ozon"),
	Yandex("yandex");

	private final String code;

	public String code() {
		return this.code;
	}

	public static PlatformEnum fromCode(String code) {
		if (code == null) return null;
		for (PlatformEnum p : values()) {
			if (p.code.equalsIgnoreCase(code)) return p;
		}
		return null;
	}

}
