package com.example.springboot.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The converter wraps a stored self-contained HTML document into a minimal OOXML
 * package whose document.xml references the HTML as an altChunk — Word converts
 * the chunk to editable content when the file is opened.
 */
class HtmlDocxConverterTest {

    private static final byte[] HTML =
            "<!DOCTYPE html><html><body><p>Generated TOR</p></body></html>"
                    .getBytes(StandardCharsets.UTF_8);

    @Test
    void producesZipPackageWithAllRequiredOoxmlParts() throws IOException {
        Map<String, byte[]> parts = unzip(HtmlDocxConverter.toDocx(HTML));

        assertTrue(parts.containsKey("[Content_Types].xml"));
        assertTrue(parts.containsKey("_rels/.rels"));
        assertTrue(parts.containsKey("word/document.xml"));
        assertTrue(parts.containsKey("word/_rels/document.xml.rels"));
        assertTrue(parts.containsKey("word/afchunk.html"));
    }

    @Test
    void embedsOriginalHtmlBytesAsTheAltChunkPart() throws IOException {
        Map<String, byte[]> parts = unzip(HtmlDocxConverter.toDocx(HTML));
        assertArrayEquals(HTML, parts.get("word/afchunk.html"));
    }

    @Test
    void documentXmlReferencesTheHtmlAltChunk() throws IOException {
        Map<String, byte[]> parts = unzip(HtmlDocxConverter.toDocx(HTML));

        String contentTypes = new String(parts.get("[Content_Types].xml"), StandardCharsets.UTF_8);
        String documentXml = new String(parts.get("word/document.xml"), StandardCharsets.UTF_8);
        String documentRels = new String(parts.get("word/_rels/document.xml.rels"), StandardCharsets.UTF_8);
        String packageRels = new String(parts.get("_rels/.rels"), StandardCharsets.UTF_8);

        assertTrue(contentTypes.contains("Extension=\"html\""),
                "content types must declare the html part");
        assertTrue(documentXml.contains("<w:altChunk r:id=\"htmlChunk\"/>"),
                "document body must reference the altChunk");
        assertTrue(documentRels.contains("Target=\"afchunk.html\""),
                "document rels must point at the html chunk part");
        assertTrue(documentRels.contains("relationships/aFChunk"),
                "chunk relationship must use the aFChunk type");
        assertTrue(packageRels.contains("word/document.xml"),
                "package rels must point at the main document part");
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> HtmlDocxConverter.toDocx(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> HtmlDocxConverter.toDocx(null));
    }

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                parts.put(entry.getName(), in.readAllBytes());
            }
        }
        return parts;
    }
}