import mitt from 'mitt'
import type {
  AnnouncementCloseMessage,
  AnnouncementPushMessage
} from '@/api/notify/announcement/types'

type Events = {
  // 切换语言
  'switch-language': string
  // 推送公告
  'announcement-push': AnnouncementPushMessage
  // 关闭公告
  'announcement-close': AnnouncementCloseMessage
  // 关闭当前tab页（可选传入要关闭的路径）
  'close-current-tab': string | undefined
  // 刷新SKU列表
  'refresh-sku-list': void
  // 刷新自定义退货单列表
  'refresh-custom-return-list': void
  // 刷新自定义出库单列表
  'refresh-custom-outbound-list': void
}

export const emitter = mitt<Events>()
