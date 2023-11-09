package com.license.validator.store;

import com.license.validator.entity.LicenseToken;

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
public class DataSourceLicenseStore implements LicenseStore {
    private static final String LOCK_TABLE_NAME = "p_lock";
    private final DataSource ds;

    public DataSourceLicenseStore(DataSource ds) {
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
    public LicenseToken getLicenseToken() throws IOException {
        String sql = "select * from " + LOCK_TABLE_NAME + " limit 1";
        try (Connection connection = ds.getConnection();
             PreparedStatement stat = connection.prepareStatement(sql);
             ResultSet rs = stat.executeQuery()) {
            if (rs.next()) {
                return new LicenseToken(rs.getString("serial"), rs.getLong("timestamp"));
            }
            return null;
        } catch (SQLException e) {
            throw new IOException(e);
        }

    }

    @Override
    public void storeLicenseToken(LicenseToken token) throws IOException {
        String sql = "INSERT INTO  " + LOCK_TABLE_NAME + "(serial,timestamp) VALUES (?, ?)";
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token.getSerial());
            ps.setLong(2, token.getTimestamp());
            ps.execute();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
