import com.mysql.jdbc.Connection;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by The Illsionist on 2017/11/9.
 * 数据库读取器,读取数据库信息以提供给转换器
 */
public class DBReader {

    private Connection conn = null;
	private String dbName = null;

    /**
     * 构造函数,记录数据库名称并获取连接
     * @param dbName
     */
    public DBReader(String dbName){
        if(dbName == null || dbName.matches("\\s*")){
            throw new InvalidParameterException("不合法的数据库名称");
        }
	    this.dbName = dbName;
        conn = ConnFactory.getConnection(dbName);
    }

    /**
     * 查询数据库中每张实体表的主键字段
     * @return Map 键为表名,值是字段对象
     * @throws SQLException
     */
    public HashMap<String,Column> getEntityKey() throws SQLException{
        HashMap<String,Column> entityKey = new HashMap<>();
        String sql = "select table_name,column_name,data_type from information_schema.columns " +
                "where table_schema = \'" + dbName + "\' and column_key = \'pri\' " +
                "  and table_name not in ( " +
                "select table_name " +
                "from information_schema.key_column_usage" +
                "where table_schema = \'" + dbName + "\' " +
                "  and referenced_table_name is not null )";
        PreparedStatement pstmt = (PreparedStatement)conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()){
            entityKey.put(rs.getString(1),new Column(rs.getString(2),dataTpMaps(rs.getString(3))));
        }
        rs.close();
        pstmt.close();
        return entityKey;
    }

    /**
     * 查询数据库中所有实体表的字段名和其数据类型
     * @param tbs 数据库中实体表的名字列表
     * @return 数据库中每个实体表中的字段以及对应的数据类型
     * @throws SQLException
     */
    public HashMap<String,List<Column>> tbColsWithTypes(Map<String,String> tbs) throws SQLException{
        if(tbs == null || tbs.size() == 0){
            return null;
        }
        HashMap<String,List<Column>> results = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        Iterator<String> tbNames = tbs.keySet().iterator();
        while(tbNames.hasNext()){
            builder.append("select table_name,column_name,data_type from information_schema.columns where table_name = ");
            builder.append("\'" + tbNames.next() + "\' and table_schema = \'" + dbName + "\'").append(" union ");
        }
        builder.delete(builder.length() - 7,builder.length() - 1);    //去掉最后一个 'union'
        PreparedStatement pstmt = (PreparedStatement) conn.prepareStatement(builder.toString());
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()){
            String tbName = rs.getString(1);  //当前记录表名
            if(results.containsKey(tbName)){
                results.get(tbName).add(new Column(rs.getString(2),dataTpMaps(rs.getString(3))));
            }else{
                LinkedList<Column> properties = new LinkedList<>();
                properties.add(new Column(rs.getString(2),dataTpMaps(rs.getString(3))));
                results.put(rs.getString(1),properties);
            }
        }
        rs.close();
        pstmt.close();
        return results;
    }

    /**
     * 查询在执行多对多关系表时所必须的模式信息,结果行会被分割为两个集合,一个包含了所有多对多关系表的所有非主键字段和字段取值的数据类型
     * 另外一个包含了所有多对多关系表的主键字段和该主键所引用的另外一个表的表名,结果集的列曾现这种形式:
     * table_name,column_name,referenced_table_name,isPri(1)
     * table_name,column_name,data_type,isPri(0)
     * 行的分割依据是isPri列的取值,isPri取1时该行描述了主键和所引用表的对应关系,isPri取0时描述了多对多关联表的非主键字段和数据类型
     * @param relTbToRelMap 多对多关联表的描述信息集合,其中包括了表名
     * @return 查询直接得到的ResultSet,在模型转换器中再进行两类数据的分割和处理
     * @throws SQLException
     */
    public ResultSet schemaInfoOfRelTb(Map<String,Relation> relTbToRelMap) throws SQLException{
        if(relTbToRelMap == null || relTbToRelMap.size() == 0){
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Iterator<String> relTbNames = relTbToRelMap.keySet().iterator();
        while(relTbNames.hasNext()){
            String relTbName = relTbNames.next();
            builder.append("select columns.table_name as table_name,columns.column_name as column_name," +
                    "referenced_table_name,true as isPri from information_schema.columns,information_schema.key_column_usage " +
                    "where columns.column_name = key_column_usage.column_name and referenced_table_name is not null " +
                    " and columns.table_name = \'" + relTbName + "\' and columns.table_schema = \'" + dbName + "\'").append(" union ");
            builder.append("select table_name,column_name,data_type,false from information_schema.columns where " +
                    " table_name = \'" + relTbName + "\'and table_schema = \'" + dbName + "\' and column_key != \'pri\' ");
            builder.append(" union ");
        }
        builder.delete(builder.length() - 7,builder.length() - 1);  //删去最后一个 "union"
        PreparedStatement pstmt = (PreparedStatement)conn.prepareStatement(builder.toString());
        ResultSet rs = pstmt.executeQuery();
        return rs;
    }


    /**
     * 根据从数据库中取出的数据类型确定转换时使用的数据类型
     * @param dbDataType 从数据库中取出的数据类型
     * @return 转换时使用的数据类型
     */
    public DataType dataTpMaps(String dbDataType){
        DataType result = DataType.STR;
        if(dbDataType.contains("int")){
            result = DataType.INT;
        }else if(dbDataType.contains("char") || dbDataType.contains("text")){
            result = DataType.STR;
        }else if(dbDataType.contains("float") || dbDataType.contains("double")){
            result = DataType.DBL;
        }else if(dbDataType.equals("date")){
            result = DataType.DT;
        } else if(dbDataType.equals("time")){
            result = DataType.TM;
        }else if(dbDataType.equals("binary")){
            result = DataType.BIN;
        } else{
            result = DataType.DTTM;
        }
        return result;
    }

    /**
     * 查询库中某表中的全部数据
     * @param tbName 表名
     * @return 结果集
     * @throws SQLException
     */
    public ResultSet queryAll(String tbName) throws SQLException {
        String sql = "select * from " + tbName;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        return pstmt.executeQuery();
    }


}
