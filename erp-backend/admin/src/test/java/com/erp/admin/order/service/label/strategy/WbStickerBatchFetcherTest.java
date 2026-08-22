package com.erp.admin.order.service.label.strategy;

import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.request.sticker.WbGetStickersRequest;
import com.erp.admin.platform.wildberries.model.response.sticker.WbSticker;
import com.erp.admin.platform.wildberries.model.response.sticker.WbStickersResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WbStickerBatchFetcherTest {

	@Test
	void splitsRequestsIntoAtMostOneHundredOrdersAndKeepsBarcodes() {
		WildberriesClient client = mock(WildberriesClient.class);
		WbCredential credential = new WbCredential("test-key");
		when(client.getOrderStickersTyped(eq(credential), any(WbGetStickersRequest.class)))
				.thenAnswer(invocation -> {
					WbGetStickersRequest request = invocation.getArgument(1);
					List<WbSticker> stickers = request.getOrders().stream()
							.map(id -> WbSticker.builder()
									.orderId(id)
									.barcode("BAR-" + id)
									.file("FILE-" + id)
									.build())
							.collect(Collectors.toList());
					return WbStickersResponse.builder().stickers(stickers).build();
				});

		WbStickerBatchFetcher fetcher = new WbStickerBatchFetcher(client);
		List<Long> ids = LongStream.rangeClosed(1, 205).boxed().collect(Collectors.toList());

		Map<Long, WbSticker> result = fetcher.fetch(credential, ids);

		ArgumentCaptor<WbGetStickersRequest> captor =
				ArgumentCaptor.forClass(WbGetStickersRequest.class);
		verify(client, times(3)).getOrderStickersTyped(eq(credential), captor.capture());
		assertThat(captor.getAllValues())
				.extracting(request -> request.getOrders().size())
				.containsExactly(100, 100, 5);
		assertThat(result).hasSize(205);
		assertThat(result.get(205L).getBarcode()).isEqualTo("BAR-205");
	}

	@Test
	void continuesWithLaterBatchWhenOneBatchFails() {
		WildberriesClient client = mock(WildberriesClient.class);
		WbCredential credential = new WbCredential("test-key");
		when(client.getOrderStickersTyped(eq(credential), any(WbGetStickersRequest.class)))
				.thenThrow(new IllegalStateException("temporary failure"))
				.thenAnswer(invocation -> {
					WbGetStickersRequest request = invocation.getArgument(1);
					List<WbSticker> stickers = request.getOrders().stream()
							.map(id -> WbSticker.builder().orderId(id).barcode("BAR-" + id)
									.file("FILE-" + id).build())
							.collect(Collectors.toList());
					return WbStickersResponse.builder().stickers(stickers).build();
				});

		WbStickerBatchFetcher fetcher = new WbStickerBatchFetcher(client);
		List<Long> ids = LongStream.rangeClosed(1, 101).boxed().collect(Collectors.toList());

		Map<Long, WbSticker> result = fetcher.fetch(credential, ids);

		assertThat(result).containsOnlyKeys(101L);
	}
}
