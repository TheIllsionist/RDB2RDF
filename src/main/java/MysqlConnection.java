import com.mysql.jdbc.Connection;
import java.io.FileInputStream;
import java.util.Properties;
import java.sql.DriverManager;

/**
 * Created by illsionist on 11/9/17.
 * 创建指定数据库名的数据库连接
 */
public class MysqlConnection {

    private static Properties prop = null;   //数据库连接参数

    private static final String configFileName = "./src/main/resources/db.properties";  //数据库配置文件全名

    /**
     * 获取数据库连接
     * @param dbName 数据库名
     * @return 数据库连接
     * @throws Exception  IOException, ClassNotFoundException, SQLException
     */
    public static Connection getConnection(String dbName) throws Exception{
        if(prop == null){
            prop = new Properties();
            prop.load(new FileInputStream(configFileName));
            String dbDriver = prop.getProperty("dbDriver");
            Class.forName(dbDriver);
        }

        String dbUrl = prop.getProperty(dbName);
        String user = prop.getProperty("user");
        String password = prop.getProperty("password");
        Connection conn = (Connection)DriverManager.getConnection(dbUrl,user,password);
        return conn;
    }

}
