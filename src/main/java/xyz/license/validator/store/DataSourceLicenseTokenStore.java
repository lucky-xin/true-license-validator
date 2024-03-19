package xyz.license.validator.store;

import xyz.license.validator.entity.LicenseToken;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * License 校验成功缓存
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-08
 */
public class DataSourceLicenseTokenStore implements LicenseTokenStore {
    private static final String LOCK_TABLE_NAME = "p_lock";
    private final DataSource ds;
    private LicenseToken token;

    public DataSourceLicenseTokenStore(DataSource ds) {
        this.ds = ds;
        try (Connection connection = ds.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, "%", LOCK_TABLE_NAME, new String[]{"TABLE"});
            try (tables) {
                if (!tables.next()) {
                    String sql = """
                            create table p_lock
                            (
                                id bigint auto_increment comment '主键' primary key,
                                serial    text   not null,
                                timestamp bigint not null
                            ) charset = utf8mb4;
                            """;
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.execute();
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public LicenseToken get() throws IOException {
        if (token != null) {
            return token;
        }
        String sql = "select * from " + LOCK_TABLE_NAME + " limit 1";
        try (Connection connection = ds.getConnection();
             PreparedStatement stat = connection.prepareStatement(sql);
             ResultSet rs = stat.executeQuery()) {
            if (rs.next()) {
                token = new LicenseToken(rs.getString("serial"), rs.getLong("timestamp"));
            }
            return token;
        } catch (SQLException e) {
            throw new IOException(e);
        }

    }

    @Override
    public void remove() throws IOException {
        String sql = "delete from " + LOCK_TABLE_NAME + " where serial is not null";
        try (Connection connection = ds.getConnection();
             PreparedStatement stat = connection.prepareStatement(sql)) {
            stat.execute();
            token = null;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void store(LicenseToken token) throws IOException {
        String insert = "INSERT INTO  " + LOCK_TABLE_NAME + "(serial,timestamp) VALUES (?, ?)";
        String query = "SELECT 1 FROM " + LOCK_TABLE_NAME;
        String sql = insert;
        try (Connection connection = ds.getConnection();
             PreparedStatement existPs = connection.prepareStatement(query)) {
            if (existPs.executeQuery().next()) {
                sql = "UPDATE " + LOCK_TABLE_NAME + " set serial = ?, timestamp = ?";;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, token.getSerial());
                statement.setLong(2, token.getTimestamp());
                statement.execute();
            }
            this.token = token;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
