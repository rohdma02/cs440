package luther;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBViews {
    public static void main(String[] args) {
        Connection conn = null;

        try {

            conn = DriverManager.getConnection("jdbc:sqlite:data/luther/luther.sqlite");

            createViews(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createViews(Connection conn) throws SQLException {
        conn.createStatement().execute("DROP VIEW IF EXISTS studentsSection;");
        conn.createStatement().execute("DROP VIEW IF EXISTS studentsClasses;");
        conn.createStatement().execute("DROP VIEW IF EXISTS studentsMajor;");
        conn.createStatement().execute("DROP VIEW IF EXISTS studentsInfo;");
        
        conn.createStatement().execute("CREATE VIEW studentsSection AS " +
           "SELECT S.* FROM Student S " +
           "JOIN Enrollment E ON S.id = E.student " +
           "JOIN Section Sec ON E.section = Sec.id;");
        
           conn.createStatement().execute("CREATE VIEW studentsInfo AS SELECT * FROM Student WHERE id = 10;");
        
        conn.createStatement().execute("CREATE VIEW studentsMajor AS " +
           "SELECT * FROM Student WHERE major = '10';");
    }
    

}
