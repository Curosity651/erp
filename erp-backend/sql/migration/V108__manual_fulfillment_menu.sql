UPDATE sys_menu
SET title = '人工出库',
    path = 'manual-fulfillment',
    uri = 'wms/manual-fulfillment/ManualFulfillmentPage',
    update_time = NOW()
WHERE id = 162002 AND deleted = 0;
