package com.erp.admin.wms.pickpackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import cn.idev.excel.FastExcel;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/** Renders both user downloads from the exact same task snapshot. */
@Component
@AllArgsConstructor
public class PickTaskPackageRenderer {

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
	private final ObjectMapper objectMapper;

	public PickTaskPackageFiles render(PickTaskPackageSnapshot snapshot,
			Map<Long, byte[]> labels, LocalDateTime generatedAt) {
		Assert.notNull(snapshot, "任务快照不能为空");
		Assert.notNull(generatedAt, "生成时间不能为空");
		Map<Long, byte[]> safeLabels = labels == null ? new LinkedHashMap<>() : labels;
		for (PickTaskPackageSnapshot.Order order : snapshot.getOrders()) {
			Assert.isTrue(safeLabels.containsKey(order.getFulfillmentOrderId()),
					"订单缺少面单：" + order.getSourceOrderNo());
		}
		String stamp = generatedAt.format(STAMP);
		Map<String, byte[]> warehouse = buildWarehouse(snapshot, safeLabels, generatedAt, stamp);
		Map<String, byte[]> archive = buildChineseArchive(snapshot, generatedAt);
		byte[] warehouseZip = PickPackageArchive.build(warehouse);
		byte[] archiveZip = PickPackageArchive.build(archive);
		return new PickTaskPackageFiles("Sklad-" + safeName(snapshot.getTaskNo()) + "-" + stamp + ".zip",
				warehouseZip, PickPackageArchive.sha256(warehouseZip),
				"拣货任务-" + safeName(snapshot.getTaskNo()) + "-" + stamp + "-中文.zip",
				archiveZip, PickPackageArchive.sha256(archiveZip));
	}

	private Map<String, byte[]> buildWarehouse(PickTaskPackageSnapshot snapshot,
			Map<Long, byte[]> labels, LocalDateTime generatedAt, String stamp) {
		Map<String, byte[]> files = new TreeMap<>();
		files.put("00_Checklist_" + stamp + ".pdf", checklist(snapshot, generatedAt));
		Map<String, List<PickTaskPackageSnapshot.Order>> byPlatform = snapshot.getOrders().stream()
				.collect(Collectors.groupingBy(PickTaskPackageSnapshot.Order::getPlatform,
						TreeMap::new, Collectors.toList()));
		for (Map.Entry<String, List<PickTaskPackageSnapshot.Order>> platform : byPlatform.entrySet()) {
			String platformDir = safeName(platform.getKey());
			files.put(platformDir + "/Pick_List_" + platformDir + ".pdf",
					pickList(snapshot, platform.getKey(), platform.getValue()));
			Map<String, List<PickTaskPackageSnapshot.Order>> byShop = platform.getValue().stream()
					.collect(Collectors.groupingBy(PickTaskPackageSnapshot.Order::getShopCode,
							TreeMap::new, Collectors.toList()));
			for (Map.Entry<String, List<PickTaskPackageSnapshot.Order>> shop : byShop.entrySet()) {
				List<byte[]> shopLabels = shop.getValue().stream()
						.map(order -> labels.get(order.getFulfillmentOrderId())).collect(Collectors.toList());
				files.put(platformDir + "/" + safeName(shop.getKey()) + "/Labels_"
						+ safeName(shop.getKey()) + ".pdf", mergePdfs(shopLabels));
			}
		}
		files.put("manifest.json", json(manifest(snapshot, generatedAt, files)));
		return files;
	}

	private Map<String, byte[]> buildChineseArchive(PickTaskPackageSnapshot snapshot,
			LocalDateTime generatedAt) {
		Map<String, byte[]> files = new TreeMap<>();
		Map<String, Long> platformCounts = snapshot.getOrders().stream().collect(
				Collectors.groupingBy(PickTaskPackageSnapshot.Order::getPlatform, TreeMap::new,
						Collectors.counting()));
		Map<String, Integer> platformQuantities = snapshot.getOrders().stream().collect(
				Collectors.groupingBy(PickTaskPackageSnapshot.Order::getPlatform, TreeMap::new,
						Collectors.summingInt(order -> order.getLines().stream()
								.mapToInt(PickTaskPackageSnapshot.Line::getQuantity).sum())));
		List<SummaryRow> summary = platformCounts.entrySet().stream()
				.map(item -> new SummaryRow(snapshot.getTaskNo(), item.getKey(), item.getValue().intValue(),
						platformQuantities.get(item.getKey()), snapshot.getTotalQuantity(), generatedAt.toString()))
				.collect(Collectors.toList());
		List<DetailRow> details = new ArrayList<>();
		for (PickTaskPackageSnapshot.Order order : snapshot.getOrders()) {
			for (PickTaskPackageSnapshot.Line line : order.getLines()) {
				details.add(new DetailRow(order.getPlatform(), order.getShopCode(), order.getSourceOrderNo(),
						line.getLocationCode(), line.getWarehouseSkuCode(), line.getSkuCode(), line.getQuantity()));
			}
		}
		List<ExceptionRow> exceptions = snapshot.getExcludedOrders().stream()
				.map(row -> new ExceptionRow(row.getFulfillmentOrderId(), row.getSourceOrderNo(), row.getReason()))
				.collect(Collectors.toList());
		files.put("00_汇总.xlsx", excel(SummaryRow.class, "汇总", summary));
		files.put("订单与SKU明细.xlsx", excel(DetailRow.class, "明细", details));
		files.put("跳过与异常.xlsx", excel(ExceptionRow.class, "异常", exceptions));
		files.put("manifest.json", json(manifest(snapshot, generatedAt, files)));
		Map<String, String> checksums = new TreeMap<>();
		files.forEach((name, bytes) -> checksums.put(name, PickPackageArchive.sha256(bytes)));
		files.put("文件校验清单.json", json(checksums));
		return files;
	}

	private Map<String, Object> manifest(PickTaskPackageSnapshot snapshot,
			LocalDateTime generatedAt, Map<String, byte[]> files) {
		Map<String, Object> manifest = new LinkedHashMap<>();
		manifest.put("schemaVersion", 1);
		manifest.put("taskId", snapshot.getTaskId());
		manifest.put("taskNo", snapshot.getTaskNo());
		manifest.put("snapshotHash", snapshot.getSnapshotHash());
		manifest.put("generatedAt", generatedAt.toString());
		manifest.put("orderCount", snapshot.getOrders().size());
		manifest.put("excludedOrderCount", snapshot.getExcludedOrders().size());
		manifest.put("totalQuantity", snapshot.getTotalQuantity());
		Map<String, String> hashes = new TreeMap<>();
		files.forEach((name, bytes) -> hashes.put(name, PickPackageArchive.sha256(bytes)));
		manifest.put("files", hashes);
		return manifest;
	}

	private byte[] checklist(PickTaskPackageSnapshot snapshot, LocalDateTime generatedAt) {
		return simplePdf(Arrays.asList("КОМПЛЕКТ ДОКУМЕНТОВ ДЛЯ СКЛАДА",
				"Задание: " + snapshot.getTaskNo(), "Создано: " + generatedAt,
				"Заказы: " + snapshot.getOrders().size(), "Количество: " + snapshot.getTotalQuantity(),
				"Контрольная сумма: " + snapshot.getSnapshotHash()));
	}

	private byte[] pickList(PickTaskPackageSnapshot snapshot, String platform,
			List<PickTaskPackageSnapshot.Order> orders) {
		List<String> lines = new ArrayList<>();
		lines.add("ЛИСТ КОМПЛЕКТАЦИИ - " + platform);
		lines.add("Задание: " + snapshot.getTaskNo());
		lines.add("ЯЧЕЙКА | SKU СКЛАДА | SKU ПЛОЩАДКИ | КОЛ-ВО | ЗАКАЗ");
		for (PickTaskPackageSnapshot.Order order : orders) {
			for (PickTaskPackageSnapshot.Line line : order.getLines()) {
				lines.add(line.getLocationCode() + " | " + line.getWarehouseSkuCode() + " | "
						+ line.getSkuCode() + " | " + line.getQuantity() + " | " + order.getSourceOrderNo());
			}
		}
		return simplePdf(lines);
	}

	public static byte[] simplePdf(List<String> lines) {
		try (PDDocument document = new PDDocument();
				InputStream fontStream = PickTaskPackageRenderer.class.getResourceAsStream("/fonts/msyh_stamp.ttf")) {
			Assert.notNull(fontStream, "PDF字体资源不存在");
			PDType0Font font = PDType0Font.load(document, fontStream, true);
			PDPage page = null;
			PDPageContentStream content = null;
			float y = 0;
			for (String raw : wrap(lines, 92)) {
				if (content == null || y < 45) {
					if (content != null) content.close();
					page = new PDPage(PDRectangle.A4);
					document.addPage(page);
					content = new PDPageContentStream(document, page);
					content.setFont(font, 9);
					y = 800;
				}
				String text = raw == null ? "" : raw;
				content.beginText(); content.newLineAtOffset(36, y); content.showText(text); content.endText();
				y -= 14;
			}
			if (content != null) content.close();
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				document.save(out); return out.toByteArray();
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("PDF生成失败：" + ex.getMessage(), ex);
		}
	}

	private byte[] mergePdfs(List<byte[]> sources) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PDFMergerUtility merger = new PDFMergerUtility();
			for (byte[] source : sources) {
				Assert.isTrue(source != null && source.length > 4, "面单文件为空");
				merger.addSource(new ByteArrayInputStream(source));
			}
			merger.setDestinationStream(out);
			merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			return out.toByteArray();
		}
		catch (Exception ex) {
			throw new IllegalStateException("面单PDF合并失败：" + ex.getMessage(), ex);
		}
	}

	private <T> byte[] excel(Class<T> type, String sheet, List<T> rows) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			FastExcel.write(out, type).sheet(sheet).doWrite(rows);
			return out.toByteArray();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Excel生成失败：" + ex.getMessage(), ex);
		}
	}

	private byte[] json(Object value) {
		try { return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value); }
		catch (Exception ex) { throw new IllegalStateException("清单生成失败", ex); }
	}

	private static String safeName(String value) {
		String safe = value == null ? "UNKNOWN" : value.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
		return safe.isEmpty() ? "UNKNOWN" : safe;
	}

	private static List<String> wrap(List<String> lines, int maxChars) {
		List<String> result = new ArrayList<>();
		for (String value : lines) {
			String remaining = value == null ? "" : value;
			if (remaining.isEmpty()) { result.add(remaining); continue; }
			while (remaining.length() > maxChars) {
				result.add(remaining.substring(0, maxChars));
				remaining = "  " + remaining.substring(maxChars);
			}
			result.add(remaining);
		}
		return result;
	}

	@Data @NoArgsConstructor @AllArgsConstructor
	public static class SummaryRow {
		@ExcelProperty("任务号") private String taskNo;
		@ExcelProperty("平台") private String platform;
		@ExcelProperty("订单数") private Integer orderCount;
		@ExcelProperty("平台件数") private Integer platformQuantity;
		@ExcelProperty("任务总件数") private Integer taskTotalQuantity;
		@ExcelProperty("生成时间") private String generatedAt;
	}
	@Data @NoArgsConstructor @AllArgsConstructor
	public static class DetailRow {
		@ExcelProperty("平台") private String platform;
		@ExcelProperty("店铺") private String shop;
		@ExcelProperty("平台订单号") private String orderNo;
		@ExcelProperty("库位") private String location;
		@ExcelProperty("仓库SKU") private String warehouseSku;
		@ExcelProperty("平台SKU") private String platformSku;
		@ExcelProperty("数量") private Integer quantity;
	}
	@Data @NoArgsConstructor @AllArgsConstructor
	public static class ExceptionRow {
		@ExcelProperty("履约订单ID") private Long fulfillmentOrderId;
		@ExcelProperty("平台订单号") private String orderNo;
		@ExcelProperty("原因") private String reason;
	}
}
