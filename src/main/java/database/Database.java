package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    // URL подключения к H2.
    // jdbc:h2: - протокол
    // ./testdb - путь к файлу (создастся в корне проекта)
    private static final String URL = "jdbc:h2:./testdb";
    private static final String USER = "sa"; // Стандартный логин H2
    private static final String PASSWORD = ""; // Пустой пароль

    // Метод для получения соединения (будем использовать в Handler-ах)
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Метод инициализации (запускаем один раз при старте сервера)
    public static void initialize() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // SQL-запрос на создание таблицы
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(255) NOT NULL, " +
                    "password VARCHAR(255) NOT NULL" +
                    ")";

            // Отправляем запрос в базу
            stmt.execute(sql);
            System.out.println("Database initialized: Table 'users' is ready.");

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database");
        }
    }
}