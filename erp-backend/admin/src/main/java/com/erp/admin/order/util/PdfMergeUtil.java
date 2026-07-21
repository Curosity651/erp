package com.erp.admin.order.util;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * PDF 合并工具类
 * 
 * @author system
 */
public class PdfMergeUtil {

    /**
     * 合并多个 Base64 编码的 PDF 文件
     * 
     * @param base64Pdfs Base64 编码的 PDF 列表
     * @return 合并后的 PDF 字节数组
     * @throws IOException PDF 合并失败
     */
    public static byte[] mergePdfs(List<String> base64Pdfs) throws IOException {
        if (base64Pdfs == null || base64Pdfs.isEmpty()) {
            throw new IllegalArgumentException("PDF 列表不能为空");
        }
        
        // 如果只有一个 PDF，直接返回
        if (base64Pdfs.size() == 1) {
            return Base64.getDecoder().decode(base64Pdfs.get(0));
        }
        
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        try {
            // 添加所有 PDF 到合并器
            for (String base64Pdf : base64Pdfs) {
                byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
                ByteArrayInputStream input = new ByteArrayInputStream(pdfBytes);
                merger.addSource(input);
            }
            
            // 设置输出流
            merger.setDestinationStream(output);
            
            // 执行合并
            merger.mergeDocuments(null);
            
            return output.toByteArray();
            
        } finally {
            output.close();
        }
    }
}
