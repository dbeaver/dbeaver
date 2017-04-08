import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class HugeSchemaGenerator {

    public static void main(String[] args) throws SQLException {

        final String url = "jdbc:postgresql://localhost/postgres";
        final Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "1978");

        Connection conn = DriverManager.getConnection(url, props);
        conn.setAutoCommit(true);

        {
            PreparedStatement stmt = conn.prepareStatement(
                    "CREATE SCHEMA HUGE_SCHEMA");
            stmt.execute();
        }

        for (int i = 0; i < 10000; i++) {
            PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE HUGE_SCHEMA.TEST_TABLE" + i + "(ID INTEGER NOT NULL, VAL VARCHAR(64))");
            stmt.execute();
            if (i % 100 == 0) {
                System.out.println(i + " tables");
            }
        }
    }

}
