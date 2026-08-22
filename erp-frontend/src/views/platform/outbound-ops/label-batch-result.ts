export interface LabelBatchResultLike {
  status?: string
  successCount?: number
  failedCount?: number
}

export interface LabelBatchMessage {
  level: 'success' | 'warning' | 'error'
  text: string
}

export function describeLabelBatchResult(batch: LabelBatchResultLike): LabelBatchMessage {
  const success = batch.successCount || 0
  const failed = batch.failedCount || 0
  if (batch.status === 'GENERATED' && failed === 0) {
    return { level: 'success', text: `平台面单已生成，共 ${success} 单` }
  }
  if (batch.status === 'PARTIAL' || (success > 0 && failed > 0)) {
    return { level: 'warning', text: `面单部分生成成功：成功 ${success} 单，失败 ${failed} 单` }
  }
  return { level: 'error', text: `面单生成失败，共 ${failed} 单` }
}
