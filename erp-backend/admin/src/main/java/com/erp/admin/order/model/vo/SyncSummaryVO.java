package com.erp.admin.order.model.vo;

import lombok.Data;

@Data
public class SyncSummaryVO {
    private int total;
    private int processed;
    private int success;
    private int skipped;
    private int failed;

    public void merge(SyncSummaryVO other) {
        if (other == null) return;
        this.total += other.total;
        this.processed += other.processed;
        this.success += other.success;
        this.skipped += other.skipped;
        this.failed += other.failed;
    }

    public void incrProcessed() { this.processed++; }
    public void incrSuccess() { this.success++; }
    public void incrSkipped() { this.skipped++; }
    public void incrFailed() { this.failed++; }
    public void addFailed(int n) { this.failed += Math.max(0, n); }
}


