import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The Illsionist on 2017/11/9.
 * 数据库连接工厂,兼有处理关闭功能
 */
public class ConnFactory {


    /**
     * 得到指定数据库名称的数据库连接
     * @param dbName 数据库名称
     * @return 数据库连接
     */
    public static Connection getConnection(String dbName){
        Connection conn = null;
        try{
            conn = MysqlConnection.getConnection(dbName);
        }catch (Exception e){
            System.out.println("Get connection failed !");
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * 关闭数据库连接
     * @param conn 数据库连接
     */
    public static void close(Connection conn) {
        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Statement stmt) {
        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
