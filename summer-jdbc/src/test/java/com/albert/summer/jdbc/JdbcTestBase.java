package com.albert.summer.jdbc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import com.albert.summer.property.PropertyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class JdbcTestBase {

    public static final String CREATE_USER = "CREATE TABLE users (\n" +
            "    id INT AUTO_INCREMENT,\n" +
            "    name VARCHAR(255) NOT NULL,\n" +
            "    age INT,\n" +
            "    PRIMARY KEY (id)\n" +
            ");";
    public static final String CREATE_ADDRESS = "CREATE TABLE addresses (\n" +
            "    id INT AUTO_INCREMENT,\n" +
            "    userId INT NOT NULL,\n" +
            "    address VARCHAR(255) NOT NULL,\n" +
            "    zip INT,\n" +
            "    PRIMARY KEY (id)\n" +
            ");";

    public static final String INSERT_USER = "INSERT INTO users (name, age) VALUES (?, ?)";
    public static final String INSERT_ADDRESS = "INSERT INTO addresses (userId, address, zip) VALUES (?, ?, ?)";

    public static final String UPDATE_USER = "UPDATE users SET name = ?, age = ? WHERE id = ?";
    public static final String UPDATE_ADDRESS = "UPDATE addresses SET address = ?, zip = ? WHERE id = ?";

    public static final String DELETE_USER = "DELETE FROM users WHERE id = ?";
    public static final String DELETE_ADDRESS_BY_USERID = "DELETE FROM addresses WHERE userId = ?";

    public static final String SELECT_USER = "SELECT * FROM users WHERE id = ?";
    public static final String SELECT_USER_NAME = "SELECT name FROM users WHERE id = ?";
    public static final String SELECT_USER_AGE = "SELECT age FROM users WHERE id = ?";
    public static final String SELECT_ADDRESS_BY_USERID = "SELECT * FROM addresses WHERE userId = ?";


    public static final String DROP_USER = "DROP TABLE IF EXISTS  users";
    public static final String DROP_ADDRESS = "DROP TABLE IF EXISTS  addresses";


    @BeforeEach
    public void beforeEach() {
        cleanDb();
    }

    public PropertyResolver createPropertyResolver() {
        Properties ps = new Properties();
        ps.put("summer.datasource.url", "jdbc:mysql://10.10.102.83:3306/test?useSSL=false");
        ps.put("summer.datasource.username", "buynow");
        ps.put("summer.datasource.password", "buynow");
        ps.put("summer.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        PropertyResolver pr = new PropertyResolver(ps);
        return pr;
    }

    public void cleanDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://10.10.102.83:3306/test", "buynow", "buynow")) {
            PreparedStatement user = conn.prepareStatement(DROP_USER);
            PreparedStatement address = conn.prepareStatement(DROP_ADDRESS);
            user.executeUpdate();
            address.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


}
