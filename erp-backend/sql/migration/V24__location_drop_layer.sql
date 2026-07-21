-- V24 库位管理二维化：去掉「层」维度，库位结构简化为 排×列。
-- 背景：海外仓货架为平面摆放（一排排货位），无上下分层。库位编码由 A1-03-03 改为 A1-03。
-- DB 处于 dev-clean 态、无存量库位/仓库结构数据，直接删列无数据迁移风险。

ALTER TABLE wms_warehouse DROP COLUMN rack_layers;

ALTER TABLE wms_location DROP COLUMN layer_no;
