package com.emr.gds.features.ReferenceFile.persistence;

import com.emr.gds.core.db.AppDatabaseManager;
import com.emr.gds.features.ReferenceFile.application.ReferenceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

public class SqliteReferenceRepository implements ReferenceRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteReferenceRepository.class);
    private final AppDatabaseManager dbManager;
    private final Path databasePath;

    public SqliteReferenceRepository(AppDatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.databasePath = null;
        createTable();
    }

    /** Opens an isolated database, useful for tests and standalone reference stores. */
    public SqliteReferenceRepository(Path databasePath) {
        this.dbManager = null;
        this.databasePath = databasePath.toAbsolutePath();
        createTable();
    }

    private Connection getConnection() throws SQLException {
        return databasePath == null ? dbManager.getReferenceConnection()
                : DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS "references" (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                contents TEXT NOT NULL,
                directory_path TEXT NOT NULL
            );
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_references_category ON \"references\" (category)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_references_contents ON \"references\" (contents)");
        } catch (SQLException e) {
            logger.error("Error creating references table: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
    }

    @Override
    public ReferenceItem save(ReferenceItem item) {
        String sql;
        if (item.getId() == 0) {
            sql = "INSERT INTO \"references\" (category, contents, directory_path) VALUES (?, ?, ?)";
        } else {
            sql = "UPDATE \"references\" SET category = ?, contents = ?, directory_path = ? WHERE id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, item.getCategory());
            pstmt.setString(2, item.getContents());
            pstmt.setString(3, item.getDirectoryPath());

            if (item.getId() != 0) {
                pstmt.setInt(4, item.getId());
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows != 1) {
                throw new SQLException("Reference was not saved: no matching row.");
            }
            if (affectedRows > 0 && item.getId() == 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving reference item: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return item;
    }

    @Override
    public void delete(ReferenceItem item) {
        if (item.getId() == 0) {
            logger.warn("Cannot delete reference item without an ID.");
            return;
        }
        String sql = "DELETE FROM \"references\" WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, item.getId());
            if (pstmt.executeUpdate() != 1) {
                throw new SQLException("Reference was not deleted: no matching row.");
            }
        } catch (SQLException e) {
            logger.error("Error deleting reference item: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
    }

    @Override
    public List<ReferenceItem> findAll() {
        List<ReferenceItem> items = new ArrayList<>();
        String sql = "SELECT id, category, contents, directory_path FROM \"references\" ORDER BY category";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new ReferenceItem(
                    rs.getInt("id"),
                    rs.getString("category"),
                    rs.getString("contents"),
                    rs.getString("directory_path")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error finding all reference items: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return items;
    }

    @Override
    public Optional<ReferenceItem> findById(int id) {
        String sql = "SELECT id, category, contents, directory_path FROM \"references\" WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ReferenceItem(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("contents"),
                        rs.getString("directory_path")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reference item by ID: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ReferenceItem> findByCategoryAndContents(String category, String contents) {
        String sql = "SELECT id, category, contents, directory_path FROM \"references\" WHERE category = ? AND contents = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setString(2, contents);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ReferenceItem(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("contents"),
                        rs.getString("directory_path")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding reference item by category and contents: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByCategoryAndContents(String category, String contents, int excludeId) {
        String sql = "SELECT COUNT(1) FROM \"references\" WHERE category = ? AND contents = ? AND id <> ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setString(2, contents);
            pstmt.setInt(3, excludeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking duplicate references: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return false;
    }

    @Override
    public List<String> findDistinctCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM \"references\" ORDER BY category";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String category = rs.getString("category");
                if (category != null && !category.isEmpty()) {
                    categories.add(category);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding distinct categories: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return categories;
    }

    @Override
    public List<ReferenceItem> search(String query, String category) {
        List<ReferenceItem> items = new ArrayList<>();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "" : category.trim();
        String sql = """
            SELECT id, category, contents, directory_path
            FROM "references"
            WHERE (? = '' OR LOWER(category) LIKE ? ESCAPE '\\' OR LOWER(contents) LIKE ? ESCAPE '\\' OR LOWER(directory_path) LIKE ? ESCAPE '\\')
              AND (? = '' OR category = ?)
            ORDER BY category
            """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String likeQuery = "%" + normalizedQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            pstmt.setString(1, normalizedQuery);
            pstmt.setString(2, likeQuery);
            pstmt.setString(3, likeQuery);
            pstmt.setString(4, likeQuery);
            pstmt.setString(5, normalizedCategory);
            pstmt.setString(6, normalizedCategory);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ReferenceItem(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("contents"),
                        rs.getString("directory_path")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching reference items: {}", e.getMessage(), e);
            throw new IllegalStateException("Reference database operation failed.", e);
        }
        return items;
    }
}
