package com.campus.agent.document;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DocumentTextExtractor {

    public ExtractedDocument extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = filename.toLowerCase(Locale.ROOT);
        try {
            String text;
            if (lowerName.endsWith(".pdf")) {
                text = extractPdf(file);
            } else if (lowerName.endsWith(".docx")) {
                text = extractDocx(file);
            } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
                text = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException("暂只支持 PDF、DOCX、TXT 和 Markdown 文件");
            }
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("文件中没有解析到可索引文本");
            }
            return new ExtractedDocument(filename, file.getContentType(), text.trim());
        } catch (IOException exception) {
            throw new IllegalArgumentException("文件解析失败：" + exception.getMessage(), exception);
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    public record ExtractedDocument(String filename, String contentType, String text) {
    }
}
