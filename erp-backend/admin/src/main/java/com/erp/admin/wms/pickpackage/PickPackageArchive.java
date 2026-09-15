package com.erp.admin.wms.pickpackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Builds and verifies deterministic, path-safe pick-task archives. */
public final class PickPackageArchive {

	private static final long ZIP_EPOCH = 315532800000L;

	private PickPackageArchive() {
	}

	public static byte[] build(Map<String, byte[]> sourceMembers) {
		if (sourceMembers == null || sourceMembers.isEmpty()) {
			throw new IllegalArgumentException("压缩包不能为空");
		}
		Map<String, byte[]> members = normalize(sourceMembers);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
			zip.setLevel(6);
			for (Map.Entry<String, byte[]> member : members.entrySet()) {
				ZipEntry entry = new ZipEntry(member.getKey());
				entry.setTime(ZIP_EPOCH);
				zip.putNextEntry(entry);
				zip.write(member.getValue());
				zip.closeEntry();
			}
			zip.finish();
			return output.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("压缩包生成失败", ex);
		}
	}

	public static Map<String, byte[]> read(byte[] archive) {
		if (archive == null || archive.length == 0) {
			throw new IllegalArgumentException("压缩包不能为空");
		}
		Map<String, byte[]> members = new LinkedHashMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive),
				StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				String name = safePath(entry.getName());
				if (members.containsKey(name)) {
					throw new IllegalArgumentException("压缩包存在重复成员: " + name);
				}
				ByteArrayOutputStream data = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				int length;
				while ((length = zip.read(buffer)) >= 0) {
					if (length > 0) data.write(buffer, 0, length);
				}
				members.put(name, data.toByteArray());
			}
			return members;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("压缩包无法读取", ex);
		}
	}

	public static Verification verify(byte[] archive, Set<String> expectedMembers) {
		Map<String, byte[]> actual = read(archive);
		Set<String> expected = new LinkedHashSet<>();
		for (String name : expectedMembers == null ? Collections.<String>emptySet() : expectedMembers) {
			expected.add(safePath(name));
		}
		List<String> problems = new ArrayList<>();
		for (String name : expected) {
			if (!actual.containsKey(name)) problems.add("缺少成员: " + name);
		}
		for (String name : actual.keySet()) {
			if (!expected.contains(name)) problems.add("多余成员: " + name);
		}
		Map<String, String> hashes = new LinkedHashMap<>();
		for (Map.Entry<String, byte[]> member : actual.entrySet()) {
			hashes.put(member.getKey(), sha256(member.getValue()));
		}
		return new Verification(problems.isEmpty(), problems, hashes);
	}

	public static String sha256(byte[] value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
			return hex.toString();
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("JVM 不支持 SHA-256", ex);
		}
	}

	private static Map<String, byte[]> normalize(Map<String, byte[]> sourceMembers) {
		Map<String, byte[]> result = new TreeMap<>();
		for (Map.Entry<String, byte[]> member : sourceMembers.entrySet()) {
			String name = safePath(member.getKey());
			if (result.put(name, member.getValue() == null ? new byte[0] : member.getValue()) != null) {
				throw new IllegalArgumentException("压缩包存在重复成员: " + name);
			}
		}
		return result;
	}

	private static String safePath(String value) {
		if (value == null) throw new IllegalArgumentException("压缩包成员路径不安全");
		String path = value.replace('\\', '/').trim();
		if (path.isEmpty() || path.startsWith("/") || path.matches("^[A-Za-z]:.*")) {
			throw new IllegalArgumentException("压缩包成员路径不安全: " + value);
		}
		for (String segment : path.split("/", -1)) {
			if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
				throw new IllegalArgumentException("压缩包成员路径不安全: " + value);
			}
		}
		return path;
	}

	public static final class Verification {
		private final boolean valid;
		private final List<String> problems;
		private final Map<String, String> memberSha256;

		private Verification(boolean valid, List<String> problems, Map<String, String> memberSha256) {
			this.valid = valid;
			this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
			this.memberSha256 = Collections.unmodifiableMap(new LinkedHashMap<>(memberSha256));
		}

		public boolean isValid() { return valid; }
		public List<String> getProblems() { return problems; }
		public Map<String, String> getMemberSha256() { return memberSha256; }
	}
}
