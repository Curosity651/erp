UPDATE sys_menu
SET title = '出库作业',
    path = 'fulfillment-workbench',
    uri = 'platform/fulfillment-workbench/FulfillmentWorkbenchPage',
    hidden = 0,
    update_time = NOW()
WHERE id = 170503 AND deleted = 0;

UPDATE sys_menu
SET hidden = 1, update_time = NOW()
WHERE id = 170504 AND deleted = 0;
