# -*- coding: utf-8 -*-
"""
Yandex Market FBS 发货链路分步验证脚本
=======================================
与 ERP 后端 (YandexClient / YdOrderConfirmService) 完全相同的请求构造，
用于在改造 Java 代码前，对单个真实店铺逐步验证平台状态流转。

用法:
  python yandex_flow_probe.py init                          生成配置文件模板
  python yandex_flow_probe.py check                         [只读] 验证凭证 (GET /v2/campaigns)
  python yandex_flow_probe.py orders [--days 7] [--status PROCESSING] [--substatus STARTED]
                                                            [只读] 查订单列表
  python yandex_flow_probe.py order <orderId>               [只读] 查单个订单详情(状态/子状态/商品/配送)
  python yandex_flow_probe.py shipments [--from 2026-06-25] [--to 2026-07-09]
                                                            [只读] 查发货批次列表(状态/可用操作)
  python yandex_flow_probe.py shipment <shipmentId>         [只读] 查单个批次详情
  python yandex_flow_probe.py confirm-order <orderId> --yes [写] 装箱+状态变更(=ERP确认按钮, 两步分开打印)
  python yandex_flow_probe.py label <orderId>               [只读] 下载订单面单 PDF
  python yandex_flow_probe.py confirm-shipment <shipmentId> --signatory <YandexID> [--external-id X] --yes
                                                            [写] 确认发货批次(被注释的第三步)
  python yandex_flow_probe.py act                           [只读] 下载最近批次的电子交接单 PDF

安全说明:
  - 未加 --yes 的写操作只打印将要发送的请求, 不真正调用 (dry-run)。
  - confirm-order 会把平台订单推到 PROCESSING/READY_TO_SHIP, 不易回退, 请用测试单或最不重要的单。
  - confirm-shipment 仅在批次 availableActions 含 CONFIRM 时放行 (官方要求过了当日 cutoff 才可确认)。
"""
import argparse
import json
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

BASE_URL = "https://api.partner.market.yandex.ru"
CONFIG_FILE = Path(__file__).with_name("yandex_probe_config.json")

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


# ---------------------------------------------------------------- 基础设施

def load_config():
    if not CONFIG_FILE.exists():
        sys.exit(f"缺少配置文件 {CONFIG_FILE}\n先运行: python yandex_flow_probe.py init")
    cfg = json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
    for key in ("api_key", "business_id", "campaign_id"):
        if not cfg.get(key):
            sys.exit(f"配置文件缺少字段: {key}")
    return cfg


def http(method, path, cfg, body=None, query=None, expect_pdf=False):
    """发送请求。认证方式与 ERP YandexApiUtils 一致: Api-Key 请求头。"""
    url = BASE_URL + path
    if query:
        pairs = "&".join(f"{k}={v}" for k, v in query.items() if v is not None)
        if pairs:
            url += "?" + pairs
    data = json.dumps(body).encode("utf-8") if body is not None else None
    print(f"\n>>> {method} {url}")
    if body is not None:
        print(f">>> body: {json.dumps(body, ensure_ascii=False)}")
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Api-Key", cfg["api_key"])
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read()
            print(f"<<< HTTP {resp.status}")
            if expect_pdf:
                return raw
            return json.loads(raw.decode("utf-8")) if raw else {}
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        print(f"<<< HTTP {e.code} 错误响应:")
        print(detail)
        sys.exit(1)


def pretty(obj):
    print(json.dumps(obj, ensure_ascii=False, indent=2))


def banner(text):
    print("\n" + "=" * 62)
    print(f"  {text}")
    print("=" * 62)


# ---------------------------------------------------------------- 只读步骤

def cmd_init(_args):
    if CONFIG_FILE.exists():
        sys.exit(f"{CONFIG_FILE} 已存在, 为防覆盖请手动编辑或删除后重试")
    CONFIG_FILE.write_text(json.dumps({
        "api_key": "填 shop 表 credential 里的 api_key",
        "business_id": 0,
        "campaign_id": 0,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"已生成 {CONFIG_FILE}, 请填入 shop 表 credential JSON 中的三个值。")


def cmd_check(args):
    cfg = load_config()
    banner("步骤0 [只读] 验证凭证 — GET /v2/campaigns (同 ERP validateCredential)")
    resp = http("GET", "/v2/campaigns", cfg)
    for c in resp.get("campaigns", []):
        mark = "  <-- 配置中的店铺" if c.get("id") == cfg["campaign_id"] else ""
        print(f"  campaignId={c.get('id')}  domain={c.get('domain')}  "
              f"placementType={c.get('placementType')}{mark}")
    print("\n凭证有效。placementType 应为 FBS。")


def fetch_orders(cfg, order_ids=None, statuses=None, substatuses=None, days=None, limit=50):
    """POST /v1/businesses/{businessId}/orders — 与 ERP YandexGetOrdersRequest 同构。"""
    body = {"campaignIds": [cfg["campaign_id"]]}
    if order_ids:
        body["orderIds"] = order_ids
    if statuses:
        body["statuses"] = statuses
    if substatuses:
        body["substatuses"] = substatuses
    if days:
        body["dates"] = {
            "creationDateFrom": (date.today() - timedelta(days=days)).isoformat(),
            "creationDateTo": date.today().isoformat(),
        }
    return http("POST", f"/v1/businesses/{cfg['business_id']}/orders", cfg,
                body=body, query={"limit": limit})


def print_order_row(o):
    delivery = o.get("delivery") or {}
    shipment = delivery.get("shipment") or {}
    print(f"  orderId={o.get('orderId')}  status={o.get('status')}/{o.get('substatus')}  "
          f"创建={o.get('creationDate','?')}  发货日={shipment.get('shipmentDate','-')}  "
          f"shipmentId={shipment.get('id','-')}")


def cmd_orders(args):
    cfg = load_config()
    banner("步骤1 [只读] 订单列表 — POST /v1/businesses/{businessId}/orders")
    resp = fetch_orders(cfg,
                        statuses=args.status or None,
                        substatuses=args.substatus or None,
                        days=args.days, limit=args.limit)
    orders = resp.get("orders", [])
    print(f"\n共 {len(orders)} 单 (单页):")
    for o in orders:
        print_order_row(o)
    token = (resp.get("paging") or {}).get("nextPageToken")
    if token:
        print(f"\n还有下一页 nextPageToken={token} (本脚本只看单页, 可加 --limit)")
    if args.raw:
        pretty(resp)


def cmd_order(args):
    cfg = load_config()
    banner(f"步骤2 [只读] 订单详情 orderId={args.order_id}")
    resp = fetch_orders(cfg, order_ids=[args.order_id])
    orders = resp.get("orders", [])
    if not orders:
        sys.exit("平台未返回该订单, 检查 orderId 与 campaign 是否匹配")
    o = orders[0]
    print_order_row(o)
    print("\n商品明细 (装箱时 ERP 用的就是 items[].id / count):")
    for it in o.get("items", []):
        print(f"  itemId={it.get('id')}  offerId={it.get('offerId')}  "
              f"count={it.get('count')}  {it.get('offerName','')}")
    if args.raw:
        pretty(o)


def cmd_shipments(args):
    cfg = load_config()
    banner("步骤3 [只读] 发货批次列表 — PUT /v2/campaigns/{campaignId}/first-mile/shipments")
    date_from = args.date_from or (date.today() - timedelta(days=7)).isoformat()
    date_to = args.date_to or (date.today() + timedelta(days=7)).isoformat()
    body = {"dateFrom": date_from, "dateTo": date_to}
    resp = http("PUT", f"/v2/campaigns/{cfg['campaign_id']}/first-mile/shipments",
                cfg, body=body, query={"limit": args.limit})
    result = resp.get("result") or {}
    shipments = result.get("shipments", [])
    print(f"\n{date_from} ~ {date_to} 共 {len(shipments)} 个批次:")
    for s in shipments:
        print(f"  shipmentId={s.get('id')}  计划日={s.get('planIntervalFrom','?')}"
              f"  status={s.get('status')}  订单数={len(s.get('orderIds', []))}"
              f"  可用操作={s.get('availableActions', [])}")
    print("\n观察要点: 状态是否停留在 OUTBOUND_READY_FOR_CONFIRMATION;"
          " availableActions 是否含 CONFIRM。")
    if args.raw:
        pretty(resp)


def cmd_shipment(args):
    cfg = load_config()
    banner(f"步骤4 [只读] 批次详情 shipmentId={args.shipment_id}")
    resp = http("GET",
                f"/v2/campaigns/{cfg['campaign_id']}/first-mile/shipments/{args.shipment_id}",
                cfg)
    pretty(resp)


# ---------------------------------------------------------------- 写操作步骤

def cmd_confirm_order(args):
    cfg = load_config()
    banner(f"步骤5 [写] 订单确认 orderId={args.order_id} — 复刻 ERP YdOrderConfirmService 两步")
    resp = fetch_orders(cfg, order_ids=[args.order_id])
    orders = resp.get("orders", [])
    if not orders:
        sys.exit("平台未返回该订单")
    o = orders[0]
    print("确认前平台状态:")
    print_order_row(o)
    if o.get("status") != "PROCESSING":
        print(f"\n警告: 订单状态为 {o.get('status')}, ERP 只对 PROCESSING(STARTED) 的单做确认。")

    box_items = [{"id": it["id"], "fullCount": it.get("count", 1)} for it in o.get("items", [])]
    box_body = {"boxes": [{"items": box_items}]}       # 同 ERP buildBoxRequest: 所有商品一个箱
    status_body = {"order": {"status": "PROCESSING", "substatus": "READY_TO_SHIP"}}

    if not args.yes:
        print("\n[dry-run] 将要执行的两个请求 (加 --yes 才真正发送):")
        print(f"  1. PUT .../orders/{args.order_id}/boxes   body={json.dumps(box_body, ensure_ascii=False)}")
        print(f"  2. PUT .../orders/{args.order_id}/status  body={json.dumps(status_body, ensure_ascii=False)}")
        return

    print("\n--- Step 1/2 装箱 (setOrderBoxLayout) ---")
    http("PUT", f"/v2/campaigns/{cfg['campaign_id']}/orders/{args.order_id}/boxes",
         cfg, body=box_body)
    print("装箱成功。")

    print("\n--- Step 2/2 状态变更 → PROCESSING/READY_TO_SHIP ---")
    http("PUT", f"/v2/campaigns/{cfg['campaign_id']}/orders/{args.order_id}/status",
         cfg, body=status_body)
    print("状态变更成功。")

    print("\n确认后回查平台状态:")
    after = fetch_orders(cfg, order_ids=[args.order_id]).get("orders", [])
    if after:
        print_order_row(after[0])
    print("\n下一步观察: 运行 shipments 命令, 看该单被归入哪个批次、批次状态如何变化。")


def cmd_label(args):
    cfg = load_config()
    banner(f"步骤6 [只读] 下载面单 orderId={args.order_id}")
    pdf = http("GET",
               f"/v2/campaigns/{cfg['campaign_id']}/orders/{args.order_id}/delivery/labels",
               cfg, expect_pdf=True)
    out = Path(f"yandex_label_{args.order_id}.pdf")
    out.write_bytes(pdf)
    print(f"面单已保存: {out.resolve()}  ({len(pdf)} 字节)")


def cmd_confirm_shipment(args):
    cfg = load_config()
    banner(f"步骤7 [写] 确认发货批次 shipmentId={args.shipment_id} — 被 ERP 注释掉的第三步")
    detail = http("GET",
                  f"/v2/campaigns/{cfg['campaign_id']}/first-mile/shipments/{args.shipment_id}",
                  cfg)
    result = detail.get("result") or {}
    status = result.get("status")
    actions = result.get("availableActions", [])
    print(f"\n批次当前: status={status}  availableActions={actions}"
          f"  订单数={len(result.get('orderIds', []))}")

    if "CONFIRM" not in actions:
        sys.exit("\n中止: availableActions 不含 CONFIRM。\n"
                 "官方要求: 批次已形成且已过当日 cutoff 才能确认 (提前调会报"
                 " 'Cutoff time for shipments has not been reached yet')。\n"
                 "请在 cutoff 之后重试, 或先用 shipment 命令观察状态。")

    body = {}
    if args.external_id:
        body["externalShipmentId"] = args.external_id
    if args.signatory:
        body["signatory"] = args.signatory

    if not args.yes:
        print(f"\n[dry-run] 将要执行 (加 --yes 才真正发送):")
        print(f"  POST .../first-mile/shipments/{args.shipment_id}/confirm"
              f"  body={json.dumps(body, ensure_ascii=False)}")
        return

    http("POST",
         f"/v2/campaigns/{cfg['campaign_id']}/first-mile/shipments/{args.shipment_id}/confirm",
         cfg, body=body)
    print("\n批次确认成功。")
    print("下一步观察: 1) shipment 命令看批次状态是否 → OUTBOUND_CONFIRMED/OUTBOUND_SIGNED;"
          " 2) act 命令下载交接单; 3) orders 命令看订单 substatus 是否 → SHIPPED / status → DELIVERY。")


def cmd_act(_args):
    cfg = load_config()
    banner("步骤8 [只读] 下载电子交接单 (акт приема-передачи)")
    pdf = http("GET",
               f"/v2/campaigns/{cfg['campaign_id']}/shipments/reception-transfer-act",
               cfg, expect_pdf=True)
    out = Path("yandex_reception_transfer_act.pdf")
    out.write_bytes(pdf)
    print(f"交接单已保存: {out.resolve()}  ({len(pdf)} 字节)")


# ---------------------------------------------------------------- main

def main():
    p = argparse.ArgumentParser(description="Yandex FBS 发货链路分步验证")
    sub = p.add_subparsers(dest="cmd", required=True)

    sub.add_parser("init")
    sub.add_parser("check")

    sp = sub.add_parser("orders")
    sp.add_argument("--days", type=int, default=14, help="按创建日期回溯天数, 默认14")
    sp.add_argument("--status", action="append", help="可多次: PROCESSING/DELIVERY/CANCELLED...")
    sp.add_argument("--substatus", action="append", help="可多次: STARTED/READY_TO_SHIP/SHIPPED...")
    sp.add_argument("--limit", type=int, default=50)
    sp.add_argument("--raw", action="store_true", help="打印完整 JSON")

    sp = sub.add_parser("order")
    sp.add_argument("order_id", type=int)
    sp.add_argument("--raw", action="store_true")

    sp = sub.add_parser("shipments")
    sp.add_argument("--from", dest="date_from", help="yyyy-MM-dd, 默认今天-7")
    sp.add_argument("--to", dest="date_to", help="yyyy-MM-dd, 默认今天+7")
    sp.add_argument("--limit", type=int, default=50)
    sp.add_argument("--raw", action="store_true")

    sp = sub.add_parser("shipment")
    sp.add_argument("shipment_id", type=int)

    sp = sub.add_parser("confirm-order")
    sp.add_argument("order_id", type=int)
    sp.add_argument("--yes", action="store_true", help="真正执行(否则 dry-run)")

    sp = sub.add_parser("label")
    sp.add_argument("order_id", type=int)

    sp = sub.add_parser("confirm-shipment")
    sp.add_argument("shipment_id", type=int)
    sp.add_argument("--signatory", help="签署人 Yandex ID 登录名(不含 @yandex.ru)")
    sp.add_argument("--external-id", help="卖家系统内的批次外部 ID, 可选")
    sp.add_argument("--yes", action="store_true", help="真正执行(否则 dry-run)")

    sub.add_parser("act")

    args = p.parse_args()
    {
        "init": cmd_init, "check": cmd_check, "orders": cmd_orders, "order": cmd_order,
        "shipments": cmd_shipments, "shipment": cmd_shipment,
        "confirm-order": cmd_confirm_order, "label": cmd_label,
        "confirm-shipment": cmd_confirm_shipment, "act": cmd_act,
    }[args.cmd](args)


if __name__ == "__main__":
    main()
