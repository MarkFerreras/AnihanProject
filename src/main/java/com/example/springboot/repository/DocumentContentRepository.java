package com.example.springboot.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

/**
 * Streams exactly one document's BLOB at a time via a parameterized
 * single-row JDBC query — never a {@code List<Document>} or an in-memory
 * archive buffer. Bounds ZIP export memory to roughly one document's size
 * plus bookkeeping, per spec §5.
 */
@Repository
public class DocumentContentRepository {

    private static final String SQL = "SELECT content_data FROM documents WHERE document_id = ?";
    private static final int BUFFER_SIZE = 8 * 1024;

    private final DataSource dataSource;

    public DocumentContentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void copyTo(Integer documentId, OutputStream output) throws IOException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NoSuchElementException("Document not found: " + documentId);
                }
                try (InputStream content = resultSet.getBinaryStream("content_data")) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = content.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IOException("Failed to read content for document " + documentId, e);
        }
    }
}
