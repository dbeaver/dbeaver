import com.mysql.jdbc.ConnectionImpl;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLErrorsTest {

    public static void main(String[] args) throws Exception
    {
        ConnectionImpl con = (ConnectionImpl) DriverManager.getConnection("jdbc:mysql://localhost/sa", "root", "1978");
        {
            System.out.println("SHOW VARIABLES");
            PreparedStatement stat = con.prepareStatement("SHOW VARIABLES LIKE '%char%'");
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1) + "=" + rs.getString(2));
            }
        }
        try {
            System.out.println("SELECT * from tablica");
            PreparedStatement stat = con.prepareStatement("SELECT * from tablica");
            stat.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        try {
            System.out.println("SELECT * from таблица");
            PreparedStatement stat = con.prepareStatement("SELECT * from таблица");
            stat.executeQuery();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
