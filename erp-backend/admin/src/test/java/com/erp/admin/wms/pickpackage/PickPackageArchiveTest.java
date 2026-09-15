package com.erp.admin.wms.pickpackage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickPackageArchiveTest {

	@Test
	void archive_is_deterministic_and_verifies_every_expected_member() {
		Map<String, byte[]> members = new LinkedHashMap<>();
		members.put("00_Checklist.pdf", "check".getBytes(StandardCharsets.UTF_8));
		members.put("OZON/HOME/Labels_HOME.pdf", "label".getBytes(StandardCharsets.UTF_8));

		byte[] first = PickPackageArchive.build(members);
		byte[] second = PickPackageArchive.build(members);
		PickPackageArchive.Verification result = PickPackageArchive.verify(first,
				new LinkedHashSet<>(members.keySet()));

		assertThat(first).isEqualTo(second);
		assertThat(result.isValid()).isTrue();
		assertThat(result.getProblems()).isEmpty();
		assertThat(result.getMemberSha256()).containsOnlyKeys(members.keySet());
		assertThat(PickPackageArchive.read(first)).containsEntry("00_Checklist.pdf",
				"check".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void archive_rejects_unsafe_or_duplicate_normalized_member_paths() {
		Map<String, byte[]> unsafe = new LinkedHashMap<>();
		unsafe.put("../secret.txt", new byte[] { 1 });
		assertThatThrownBy(() -> PickPackageArchive.build(unsafe))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不安全");

		Map<String, byte[]> duplicate = new LinkedHashMap<>();
		duplicate.put("OZON\\labels.pdf", new byte[] { 1 });
		duplicate.put("OZON/labels.pdf", new byte[] { 2 });
		assertThatThrownBy(() -> PickPackageArchive.build(duplicate))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("重复");
	}

	@Test
	void verification_reports_missing_and_unexpected_members() {
		Map<String, byte[]> members = new LinkedHashMap<>();
		members.put("actual.txt", new byte[] { 1 });

		PickPackageArchive.Verification result = PickPackageArchive.verify(
				PickPackageArchive.build(members),
				new LinkedHashSet<>(Arrays.asList("expected.txt")));

		assertThat(result.isValid()).isFalse();
		assertThat(result.getProblems()).anyMatch(v -> v.contains("缺少") && v.contains("expected.txt"));
		assertThat(result.getProblems()).anyMatch(v -> v.contains("多余") && v.contains("actual.txt"));
	}
}
