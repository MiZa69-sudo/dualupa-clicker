package ru.dualupa.clicker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class Database {
    private final DuaLupaClicker plugin;
    private Connection conn;

    public Database(DuaLupaClicker plugin) { this.plugin = plugin; }

    public void connect() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("database.port", 5432);
        String name = plugin.getConfig().getString("database.name", "dualupa");
        String user = plugin.getConfig().getString("database.user", "dualupa");
        String pass = plugin.getConfig().getString("database.password", "");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + name;
        conn = DriverManager.getConnection(url, user, pass);
        plugin.getLogger().info("БД подключена");
    }

    public void disconnect() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignored) {}
    }

    public Connection get() {
        try {
            if (conn == null || conn.isClosed()) connect();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Переподключение не удалось", e);
        }
        return conn;
    }

    public PreparedStatement prepare(String sql) throws SQLException {
        return get().prepareStatement(sql);
    }
}
