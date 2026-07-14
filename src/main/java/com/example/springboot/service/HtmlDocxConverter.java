package com.example.springboot.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts a stored self-contained HTML document (generated TOR / Form IX) into a
 * .docx that opens as an editable Word document.
 *
 * Uses the OOXML "alternative format chunk" (altChunk) mechanism: a minimal
 * WordprocessingML package embeds the HTML bytes as a chunk part that Word
 * transforms into native, editable content when the file is opened. Pure
 * java.util.zip — no HTML rendering, no new dependencies, air-gap safe.
 */
public final class HtmlDocxConverter {

    public static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String CONTENT_TYPES_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Default Extension=\"html\" ContentType=\"text/html\"/>"
            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
            + "</Types>";

    private static final String PACKAGE_RELS_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
            + "</Relationships>";

    private static final String DOCUMENT_RELS_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"htmlChunk\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/aFChunk\" Target=\"afchunk.html\"/>"
            + "</Relationships>";

    /** A4 page (11906 x 16838 twips) with the same 9mm/11mm margins the print CSS uses. */
    private static final String DOCUMENT_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<w:body>"
            + "<w:altChunk r:id=\"htmlChunk\"/>"
            + "<w:sectPr>"
            + "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
            + "<w:pgMar w:top=\"510\" w:right=\"624\" w:bottom=\"510\" w:left=\"624\" w:header=\"0\" w:footer=\"0\" w:gutter=\"0\"/>"
            + "</w:sectPr>"
            + "</w:body>"
            + "</w:document>";

    private HtmlDocxConverter() {
    }

    public static byte[] toDocx(byte[] htmlBytes) {
        if (htmlBytes == null || htmlBytes.length == 0) {
            throw new IllegalArgumentException("The document has no content to convert.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            putEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML.getBytes(StandardCharsets.UTF_8));
            putEntry(zip, "_rels/.rels", PACKAGE_RELS_XML.getBytes(StandardCharsets.UTF_8));
            putEntry(zip, "word/document.xml", DOCUMENT_XML.getBytes(StandardCharsets.UTF_8));
            putEntry(zip, "word/_rels/document.xml.rels", DOCUMENT_RELS_XML.getBytes(StandardCharsets.UTF_8));
            putEntry(zip, "word/afchunk.html", htmlBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to package the document as .docx", e);
        }
        return out.toByteArray();
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}