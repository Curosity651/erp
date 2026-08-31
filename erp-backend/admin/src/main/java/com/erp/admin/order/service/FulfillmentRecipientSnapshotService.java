package com.erp.admin.order.service;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FulfillmentRecipientSnapshotService {

	private static final List<String> NAME_KEYS = Arrays.asList(
			"recipient_name", "customer_name", "buyer_name", "addressee", "full_name");
	private static final List<String> PHONE_KEYS = Arrays.asList(
			"recipient_phone", "customer_phone", "buyer_phone", "phone");
	private static final List<String> ADDRESS_KEYS = Arrays.asList(
			"recipient_address", "delivery_address", "address_full", "full_address", "address");

	private final ObjectMapper objectMapper;

	public Snapshot from(ErpOrder order) {
		Snapshot snapshot = new Snapshot();
		if (order == null || !StringUtils.hasText(order.getRawJson())) return snapshot;
		try {
			JsonNode root = objectMapper.readTree(order.getRawJson());
			snapshot.setName(findText(root, NAME_KEYS));
			snapshot.setPhone(findText(root, PHONE_KEYS));
			snapshot.setAddress(findText(root, ADDRESS_KEYS));
		}
		catch (Exception ignored) {
			// A malformed platform snapshot must not prevent warehouse submission.
		}
		return snapshot;
	}

	private String findText(JsonNode node, List<String> keys) {
		if (node == null) return null;
		if (node.isObject()) {
			for (String key : keys) {
				JsonNode value = node.get(key);
				String text = asText(value);
				if (StringUtils.hasText(text)) return text;
			}
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				String text = findText(fields.next().getValue(), keys);
				if (StringUtils.hasText(text)) return text;
			}
		}
		else if (node.isArray()) {
			for (JsonNode child : node) {
				String text = findText(child, keys);
				if (StringUtils.hasText(text)) return text;
			}
		}
		return null;
	}

	private String asText(JsonNode value) {
		if (value == null || value.isNull()) return null;
		if (value.isTextual() || value.isNumber()) return value.asText().trim();
		return null;
	}

	@Data
	public static class Snapshot {
		private String name;
		private String phone;
		private String address;
	}
}
