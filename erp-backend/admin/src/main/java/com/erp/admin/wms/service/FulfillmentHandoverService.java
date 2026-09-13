package com.erp.admin.wms.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.SysFileService;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.mapper.WmsOutboundHandoverOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentHandoverDTO;
import com.erp.admin.wms.model.dto.OutboundHandoverCreateDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsOutboundHandoverOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.model.qo.FulfillmentShippingQuery;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentShippingOrderVO;
import com.erp.admin.wms.model.vo.OutboundHandoverOrderVO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentHandoverService {

	public static final String READY_HANDOVER = "READY_HANDOVER";
	public static final String HANDED_OVER = "HANDED_OVER";
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	private final WmsOutboundHandoverOrderMapper handoverMapper;
	private final WmsFulfillmentOrderMapper fulfillmentMapper;
	private final SysFileService fileService;
	private final FulfillmentShippingService shippingService;
	private final TenantIdentityService tenantIdentityService;

	public PageResult<OutboundHandoverOrderVO> page(PageParam pageParam, FulfillmentShippingQuery query) {
		IPage<OutboundHandoverOrderVO> page = PageUtil.prodPage(pageParam);
		handoverMapper.selectHandoverPage(page,
				query == null ? new FulfillmentShippingQuery() : query, currentPlatformTenantId());
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

	public List<FulfillmentShippingOrderVO> availableFulfillments() {
		return handoverMapper.selectAvailableFulfillments(currentPlatformTenantId());
	}

	public List<String> recentDestinations() {
		return handoverMapper.selectRecentDestinations(currentPlatformTenantId());
	}

	public OutboundHandoverOrderVO detail(Long id) {
		OutboundHandoverOrderVO order = handoverMapper.selectHandoverDetail(id, currentPlatformTenantId());
		Assert.notNull(order, "出库交接单不存在");
		return order;
	}

	@Transactional(rollbackFor = Exception.class)
	public Long create(OutboundHandoverCreateDTO dto, Long userId) {
		Long tenantId = currentPlatformTenantId();
		WmsFulfillmentOrder fulfillment = fulfillmentMapper.selectForUpdate(dto.getFulfillmentOrderId());
		Assert.notNull(fulfillment, "选择的履约单不存在");
		Assert.isTrue(tenantId.equals(fulfillment.getTenantId()), "不能关联其他平台的履约单");
		Assert.isTrue(fulfillment.getFulfillmentStatus() == FulfillmentStatus.PACKED,
				"只能选择已经拣货并完成打包的货物");
		Long count = handoverMapper.selectCount(Wrappers.<WmsOutboundHandoverOrder>lambdaQuery()
				.eq(WmsOutboundHandoverOrder::getFulfillmentOrderId, dto.getFulfillmentOrderId()));
		Assert.isTrue(count == 0, "该履约单已经创建过出库交接单");

		List<Long> photoIds = normalizePhotoIds(dto.getPhotoFileIds());
		validatePhotos(photoIds);
		LocalDateTime now = LocalDateTime.now();
		WmsOutboundHandoverOrder order = new WmsOutboundHandoverOrder();
		order.setTenantId(tenantId);
		order.setHandoverNo(newHandoverNo(now));
		order.setFulfillmentOrderId(dto.getFulfillmentOrderId());
		order.setHandoverStatus(READY_HANDOVER);
		copy(dto, photoIds, order);
		order.setVersion(0);
		order.setCreateBy(userId);
		order.setCreateTime(now);
		order.setUpdateBy(userId);
		order.setUpdateTime(now);
		Assert.isTrue(handoverMapper.insert(order) == 1, "出库交接单创建失败");
		syncFulfillment(fulfillment, dto, photoIds, userId);
		return order.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public void save(Long id, FulfillmentHandoverDTO dto, Long userId) {
		Long tenantId = currentPlatformTenantId();
		WmsOutboundHandoverOrder order = handoverMapper.selectForUpdate(id, tenantId);
		Assert.notNull(order, "出库交接单不存在");
		List<Long> photoIds = normalizePhotoIds(dto.getPhotoFileIds());
		validatePhotos(photoIds);
		copy(dto, photoIds, order);
		order.setUpdateBy(userId);
		order.setUpdateTime(LocalDateTime.now());
		order.setVersion((order.getVersion() == null ? 0 : order.getVersion()) + 1);
		handoverMapper.updateById(order);

		WmsFulfillmentOrder fulfillment = fulfillmentMapper.selectById(order.getFulfillmentOrderId());
		Assert.notNull(fulfillment, "关联的履约单不存在");
		syncFulfillment(fulfillment, dto, photoIds, userId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void confirm(Long id, FulfillmentHandoverDTO dto, Long userId) {
		OutboundHandoverOrderVO before = detail(id);
		Assert.isTrue(READY_HANDOVER.equals(before.getHandoverStatus()), "该交接单已经完成交接");
		save(id, dto, userId);
		FulfillmentBatchResultVO result = shippingService.ship(
				java.util.Collections.singletonList(before.getFulfillmentOrderId()), userId);
		String failure = result.getFailures().get(before.getFulfillmentOrderId());
		Assert.isTrue(failure == null, failure == null ? "交接失败" : failure);

		WmsOutboundHandoverOrder order = handoverMapper.selectById(id);
		Assert.notNull(order, "出库交接单不存在");
		LocalDateTime now = LocalDateTime.now();
		order.setHandoverStatus(HANDED_OVER);
		order.setHandoverTime(now);
		order.setHandoverBy(userId);
		order.setUpdateBy(userId);
		order.setUpdateTime(now);
		order.setVersion((order.getVersion() == null ? 0 : order.getVersion()) + 1);
		handoverMapper.updateById(order);
	}

	public List<SysFileVO> photos(Long id) {
		return fileService.getFileInfoList(parsePhotoIds(detail(id).getLogisticsPhotoFileIds()));
	}

	public byte[] buildPdf(Long id) {
		OutboundHandoverOrderVO order = detail(id);
		Assert.isTrue(HANDED_OVER.equals(order.getHandoverStatus()), "完成交接后才能生成PDF");
		List<SysFileVO> photos = photos(id);
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			addSummaryPage(document, order, photos);
			for (SysFileVO photo : photos) {
				addPhotoPage(document, photo);
			}
			document.save(output);
			return output.toByteArray();
		}
		catch (Exception ex) {
			throw new IllegalStateException("交接凭证PDF生成失败", ex);
		}
	}

	private void copy(FulfillmentHandoverDTO dto, List<Long> photoIds,
			WmsOutboundHandoverOrder order) {
		order.setVehiclePlate(dto.getVehiclePlate().trim());
		order.setDriverName(dto.getDriverName().trim());
		order.setDriverPhone(trimToNull(dto.getDriverPhone()));
		order.setDepartureTime(dto.getDepartureTime());
		order.setDestination(dto.getDestination().trim());
		order.setFreightCost(dto.getFreightCost());
		order.setCurrency(hasText(dto.getCurrency()) ? dto.getCurrency().trim().toUpperCase() : "CNY");
		order.setLogisticsPhotoFileIds(photoIds.stream().map(String::valueOf)
				.collect(Collectors.joining(",")));
		order.setRemark(trimToNull(dto.getRemark()));
	}

	private void syncFulfillment(WmsFulfillmentOrder fulfillment, FulfillmentHandoverDTO dto,
			List<Long> photoIds, Long userId) {
		fulfillment.setVehiclePlate(dto.getVehiclePlate().trim());
		fulfillment.setDriverName(dto.getDriverName().trim());
		fulfillment.setDriverPhone(trimToNull(dto.getDriverPhone()));
		fulfillment.setDepartureTime(dto.getDepartureTime());
		fulfillment.setHandoverDestination(dto.getDestination().trim());
		fulfillment.setRecordedFreightCost(dto.getFreightCost());
		fulfillment.setRecordedFreightCurrency(
				hasText(dto.getCurrency()) ? dto.getCurrency().trim().toUpperCase() : "CNY");
		fulfillment.setLogisticsPhotoFileIds(photoIds.stream().map(String::valueOf)
				.collect(Collectors.joining(",")));
		fulfillment.setHandoverRemark(trimToNull(dto.getRemark()));
		fulfillment.setUpdateBy(userId);
		fulfillmentMapper.updateById(fulfillment);
	}

	private String newHandoverNo(LocalDateTime now) {
		return "HO" + NO_TIME.format(now)
				+ UUID.randomUUID().toString().substring(0, 5).toUpperCase();
	}

	private Long currentPlatformTenantId() {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		Assert.isTrue(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identity.getIdentityType()),
				"只有海外仓平台可以维护出库交接单");
		return identity.getTenantId();
	}

	private void addSummaryPage(PDDocument document, OutboundHandoverOrderVO order,
			List<SysFileVO> photos) throws Exception {
		BufferedImage canvas = new BufferedImage(1191, 1684, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font title = new Font("Microsoft YaHei", Font.BOLD, 44);
		Font body = new Font("Microsoft YaHei", Font.PLAIN, 27);
		graphics.setColor(Color.BLACK);
		graphics.setFont(title);
		graphics.drawString("出库交接凭证", 430, 110);
		graphics.setFont(body);
		int y = 200;
		y = line(graphics, "交接单号：" + value(order.getHandoverNo()), y);
		y = line(graphics, "履约单号：" + value(order.getFulfillmentNo()), y);
		y = line(graphics, "来源订单：" + value(order.getSourceOrderNo()), y);
		y = line(graphics, "货主：" + value(order.getOwnerName()), y);
		y = line(graphics, "承运信息：" + value(order.getCarrierName()) + " / "
				+ value(order.getShippingMethod()), y);
		y = line(graphics, "跟踪号：" + value(order.getTrackingNo()), y);
		y = line(graphics, "车牌：" + value(order.getVehiclePlate()), y);
		y = line(graphics, "司机：" + value(order.getDriverName()) + "  "
				+ value(order.getDriverPhone()), y);
		y = line(graphics, "发车时间：" + format(order.getDepartureTime()), y);
		y = line(graphics, "目的地：" + value(order.getDestination()), y);
		y = line(graphics, "记录运费：" + value(order.getFreightCost()) + " "
				+ value(order.getCurrency()), y);
		y = line(graphics, "交接时间：" + format(order.getHandoverTime()), y);
		y = line(graphics, "物流照片：" + photos.size() + " 张", y);
		line(graphics, "备注：" + value(order.getRemark()), y);
		graphics.setColor(new Color(90, 90, 90));
		graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 21));
		graphics.drawString("说明：本凭证中的运费、油费及司机月结仅作运输台账记录，不进入系统计费。",
				90, 1570);
		graphics.dispose();
		addImageAsPage(document, canvas);
	}

	private int line(Graphics2D graphics, String text, int y) {
		graphics.drawString(text, 100, y);
		return y + 75;
	}

	private void addPhotoPage(PDDocument document, SysFileVO photo) throws Exception {
		URLConnection connection = new URL(photo.getUrl()).openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(10000);
		try (InputStream input = connection.getInputStream()) {
			BufferedImage image = ImageIO.read(input);
			if (image != null) {
				addImageAsPage(document, image);
			}
		}
	}

	private void addImageAsPage(PDDocument document, BufferedImage image) throws Exception {
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
		PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
		float scale = Math.min(PDRectangle.A4.getWidth() / image.getWidth(),
				PDRectangle.A4.getHeight() / image.getHeight());
		float width = image.getWidth() * scale;
		float height = image.getHeight() * scale;
		try (org.apache.pdfbox.pdmodel.PDPageContentStream stream =
				new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
			stream.drawImage(pdImage, (PDRectangle.A4.getWidth() - width) / 2,
					(PDRectangle.A4.getHeight() - height) / 2, width, height);
		}
	}

	private void validatePhotos(List<Long> ids) {
		Assert.isTrue(ids.size() <= 6, "物流照片最多上传6张");
		List<SysFileVO> files = fileService.getFileInfoList(ids);
		Assert.isTrue(files.size() == ids.size(), "存在无效的物流照片");
		Assert.isTrue(files.stream().allMatch(file -> file.getContentType() != null
				&& file.getContentType().toLowerCase().startsWith("image/")),
				"物流凭证只允许图片文件");
	}

	private List<Long> normalizePhotoIds(List<Long> ids) {
		return ids == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(ids));
	}

	private List<Long> parsePhotoIds(String source) {
		List<Long> result = new ArrayList<>();
		if (!hasText(source)) {
			return result;
		}
		for (String value : source.split(",")) {
			try {
				result.add(Long.valueOf(value.trim()));
			}
			catch (NumberFormatException ignore) {
				// Ignore stale malformed IDs instead of making the whole document unreadable.
			}
		}
		return result;
	}

	private String format(LocalDateTime value) {
		return value == null ? "-" : DATE_TIME.format(value);
	}

	private String value(Object value) {
		return value == null || value.toString().trim().isEmpty() ? "-" : value.toString();
	}

	private String trimToNull(String value) {
		return hasText(value) ? value.trim() : null;
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
