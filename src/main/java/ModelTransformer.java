import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by The Illsionist on 2017/11/9.
 */
public class ModelTransformer {

    private String dbName = null;
    private DBReader dbReader = null;

    //来源于手工构造的配置信息
    private HashMap<String,String> entityTableMap = null; //单主键实体表表名列表
    private HashMap<String,String> pyToZh = null;  //中英文映射字典
    private HashMap<String,PropertyType> colJudgeDic = null;  //字段分类字典(判断字段应该作为DP,OP还是GC)
    private HashMap<String,Relation> relTbToRelMap = null;  //关联表到关系的映射
    private HashMap<String,String> entityDescMap = null;  //单主键实体表和描述列列表


    /**
     * 在进行单主键实体表的处理时存储所有单主键实体表的表名和它们拥有的字段名和字段的数据类型
     * 在进行多对多关联表的处理时存储所有多对多关联表的表名和它们拥有的非主键字段以及数据类型
     */
    private HashMap<String,List<Column>> tbColsWithTypes = null;
    //作为中间存储
    private HashMap<String,Set<String>> extraOpAndValues = null;  //单主键实体表中作为对象属性的字段所产生的额外信息
    private Set<String> extraClasses = null;                     //单主键实体表中额外抽取出的类别
    private HashMap<String,HashMap<String,String>> keyToRefTbMap = null;  //多对多关联表主键与该主键所引用的表名的对应关系

    /**
     * 模型转换器构造函数,构造时创建数据库信息读取器
     * @param dbName 数据库名
     */
    public ModelTransformer(String dbName){
        this.dbName = dbName;
        this.dbReader = new DBReader(dbName);
        extraOpAndValues = new HashMap<>();
        extraClasses = new HashSet<>();
        keyToRefTbMap = new HashMap<>();
    }

    public void setPyToZh(HashMap<String, String> pyToZh) {
        this.pyToZh = pyToZh;
    }
    public void setColJudgeDic(HashMap<String, PropertyType> colJudgeDic) {
        this.colJudgeDic = colJudgeDic;
    }
    public void setRelTbToRelMap(HashMap<String, Relation> relTbToRelMap) {
        this.relTbToRelMap = relTbToRelMap;
    }
    public void setEntityTableMap(HashMap<String,String> entityTableMap) {
        this.entityTableMap = entityTableMap;
    }
    public void setEntityDescMap(HashMap<String, String> entityDescMap) {
        this.entityDescMap = entityDescMap;
    }
    /**
     * 单主键实体表模式层的转换
     * @return 结果triple的列表
     * @throws SQLException
     */
    public List<String> transEntitySchema() throws SQLException{
        LinkedList<String> triples = new LinkedList<>();
        //从数据库中取出每一张单主键实体表的所有字段和字段取值的数据类型
        tbColsWithTypes = dbReader.tbColsWithTypes(entityTableMap);  //读取数据库并保存
        Iterator<String> tableIterator = tbColsWithTypes.keySet().iterator();
        while(tableIterator.hasNext()){
            String tbName = tableIterator.next();
            triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + " rdf:type owl:Class .");   //表名作为类名
            triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + " rdfs:label  " + "\"" + pyNametoZhName(tbName) + "\" .");  //给类加上一个可读名称
            System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + " rdf:type owl:Class .");
            System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + " rdfs:label  " + "\"" + pyNametoZhName(tbName) + "\" .");
            List<Column> cols = tbColsWithTypes.get(tbName);
            for(Column col : cols){
                PropertyType proType = colJudgeDic.containsKey(col.getColName())?colJudgeDic.get(col.getColName()):PropertyType.DP;
                col.setRdfTp(proType);           //记录当前字段的rdfType避免重复计算(很重要)
                if(proType == PropertyType.DP){  //将字段作为数据类型属性
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdf:type owl:DatatypeProperty .");
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdf:type owl:DatatypeProperty .");
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:label \"" + pyNametoZhName(col.getColName()) + "\" ."); //给属性加上一个可读名称
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:label \"" + pyNametoZhName(col.getColName()) + "\" .");
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:domain rdb:" + dbName + "." + pyNametoZhName(tbName) + " .");
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:domain rdb:" + dbName + "." + pyNametoZhName(tbName) + " .");
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:range " + dataTypeToStr(col.getDataTp()) + " .");
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:range " + dataTypeToStr(col.getDataTp()) + " .");
                }else if(proType == PropertyType.OP){  //将字段作为对象属性
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdf:type owl:ObjectProperty .");
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdf:type owl:ObjectProperty .");
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:label \"" + pyNametoZhName(col.getColName()) + "\" ."); //给属性加上一个可读名称
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:label \"" + pyNametoZhName(col.getColName()) + "\" .");
                    triples.add("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:range meta:blankNode .");
                    System.out.println("rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:range meta:blankNode .");
                    extraOpAndValues.put(col.getColName(),new HashSet<>());  //作为对象属性的字段还有其他需要处理的地方
                }else if(proType == PropertyType.GC){  //该字段描述了类型信息

                }else{             //该字段表示其值需要被转换为图片

                }
            }
        }
        return triples;
    }

    /**
     * 转换单主键实体表的行为实例层
     * @return triple的列表
     * @throws SQLException
     */
    public List<String> transEntityInstance() throws SQLException{
        LinkedList<String> triples = new LinkedList<>();
        Iterator<String> tableIterator = tbColsWithTypes.keySet().iterator();
        while(tableIterator.hasNext()){
            String tbName = tableIterator.next();
            ResultSet rs = dbReader.queryAll(tbName);  //查询该表所有行
            List<Column> cols = tbColsWithTypes.get(tbName); //得到该表的所有列
            while(rs.next()){  //对查询到的每行结果处理
                String uri = "rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + rs.getInt(entityTableMap.get(tbName));  //当前实例的uri
                triples.add(uri + " rdf:type rdb:" + dbName + "." + pyNametoZhName(tbName) + " .");  //定义当前行为一个实例
                System.out.println(uri + " rdf:type rdb:" + dbName + "." + pyNametoZhName(tbName) + " .");
                triples.add(uri + " rdfs:label \"" + rs.getString(entityDescMap.get(tbName)) + "\" .");  //加入当前实例的可读名称
                System.out.println(uri + " rdfs:label \"" + rs.getString(entityDescMap.get(tbName)) + "\" .");
                for(Column col:cols){  //声明实例的属性及值
                    if(col.getRdfTp() == PropertyType.DP){  //声明实例的某个数据类型属性的值
                        String tmpStr = uri + " rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " ";
                        switch(col.getDataTp()){  //根据字段值的数据类型做相应操作
                            case STR:tmpStr += "\"" + rs.getString(col.getColName()) + "\"^^xsd:string .";break;
                            case INT:tmpStr += rs.getInt(col.getColName()) + "^^xsd:integer .";break;
                            case DBL:tmpStr += rs.getDouble(col.getColName()) + "^^xsd:decimal .";break;
                            case DT:tmpStr += rs.getDate(col.getColName()) + "^^xsd:date .";break;
                            case TM:tmpStr += rs.getTime(col.getColName()) + "^^xsd:time .";break;
                            case DTTM:tmpStr += rs.getString(col.getColName()) + "^^xsd:dateTime .";break;
                        }
                        triples.add(tmpStr);  //加入这条triple
                        System.out.println(tmpStr);
                    }else if(col.getRdfTp() == PropertyType.OP){  //声明实例的某个对象属性的值
                        extraOpAndValues.get(col.getColName()).add(rs.getString(col.getColName()));  //先把值存起来
                        String uuid = getUUID();  //生成一个新的UUID码
                        triples.add("rdb:" + uuid + " rdf:type meta:blankNode .");
                        System.out.println("rdb:" + uuid + " rdf:type meta:blankNode .");
                        triples.add(uri + " rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdb:" + uuid + " .");
                        System.out.println(uri + " rdb:" + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()) + " rdb:" + uuid + " .");
                        triples.add("rdb:" + uuid + " meta:实例 rdb:" + dbName + "." + pyNametoZhName(col.getColName()) + "." + rs.getString(col.getColName()) + " .");
                        System.out.println("rdb:" + uuid + " meta:实例 rdb:" + dbName + "." + pyNametoZhName(col.getColName()) + "." + rs.getString(col.getColName()) + " .");
                    }else if(col.getRdfTp() == PropertyType.GC){
                        extraClasses.add(rs.getString(col.getColName()));   //先把值存起来
                        triples.add(uri + " rdf:type rdb:" + dbName + "." + rs.getString(col.getColName()) + " .");
                        System.out.println(uri + " rdf:type rdb:" + dbName + "." + rs.getString(col.getColName()) + " .");
                    }else{

                    }
                }
            }
        }
        //开始补充之前漏掉但是已经把值存起来的那些triple
        Iterator<String> opClasses = extraOpAndValues.keySet().iterator();
        while(opClasses.hasNext()){
            String className = opClasses.next();
            triples.add("rdb:" + dbName + "." + pyNametoZhName(className) + " rdf:type owl:Class .");
            System.out.println("rdb:" + dbName + "." + pyNametoZhName(className) + " rdf:type owl:Class .");
            triples.add("rdb:" + dbName + "." + pyNametoZhName(className) + " rdfs:label \"" + pyNametoZhName(className) + "\" .");  //加入类的可读名称
            System.out.println("rdb:" + dbName + "." + pyNametoZhName(className) + " rdfs:label \"" + pyNametoZhName(className) + "\" .");
            Iterator<String> values = extraOpAndValues.get(className).iterator();
            while(values.hasNext()){
                String tmpValue = values.next();
                triples.add("rdb:" + dbName + "." + pyNametoZhName(className) + "." + tmpValue + " rdf:type rdb:" + dbName + "." + pyNametoZhName(className) + " .");  //定义实例
                System.out.println("rdb:" + dbName + "." + pyNametoZhName(className) + "." + tmpValue + " rdf:type rdb:" + dbName + "." + pyNametoZhName(className) + " .");
                triples.add("rdb:" + dbName + "." + pyNametoZhName(className) + "." + tmpValue + " rdfs:label \"" + tmpValue + "\" .");  //加入当前实例的可读名称
                System.out.println("rdb:" + dbName + "." + pyNametoZhName(className) + "." + tmpValue + " rdfs:label \"" + tmpValue + "\" .");
            }
        }
        Iterator<String> gcClasses = extraClasses.iterator();
        while(gcClasses.hasNext()){
            String tmpClass = gcClasses.next();
            triples.add("rdb:" + dbName + "." + tmpClass + " rdf:type owl:Class .");
            System.out.println("rdb:" + dbName + "." + tmpClass + " rdf:type owl:Class .");
            triples.add("rdb:" + dbName + "." + tmpClass + "rdfs:label \"" + tmpClass + "\" .");  //加入当前类的可读名称
            System.out.println("rdb:" + dbName + "." + tmpClass + "rdfs:label \"" + tmpClass + "\" .");
        }
        //把所有值返回
        return triples;
    }

    /**
     * 多对多关联表模式层的转换
     * @return 结果triple的列表
     * @throws SQLException
     */
    public List<String> transRelationSchema() throws SQLException{
        LinkedList<String> triples = new LinkedList<>();
        //在复用 tbColsWithTypes 之前要先清空里面的对象
        if(tbColsWithTypes.size() != 0){
            tbColsWithTypes.clear();
        }
        DivideRelInfo();  //执行查询并将结果分开
        Iterator<String> relTbNames = relTbToRelMap.keySet().iterator();
        while(relTbNames.hasNext()){  //遍历每一张多对多关系表
            String relTbName = relTbNames.next();  //当前多对多关系表的表名
            Relation rel = relTbToRelMap.get(relTbName);
            //多对多关联表名作为关系定义
            triples.add("rdb:" + dbName + "." + rel.getRelName() + " rdf:type owl:ObjectProperty .");
            System.out.println("rdb:" + dbName + "." + rel.getRelName() + " rdf:type owl:ObjectProperty .");
            triples.add("rdb:" + dbName + "." + rel.getRelName() + " rdfs:label \"" + rel.getRelName() + "\" .");  //加入当前属性的可读名称
            System.out.println("rdb:" + dbName + "." + rel.getRelName() + " rdfs:label \"" + rel.getRelName() + "\" .");
            triples.add("rdb:" + dbName + "." + rel.getRelName() + " rdfs:domain rdb:" + dbName + "." + pyNametoZhName(rel.getFromTb()) + " .");
            System.out.println("rdb:" + dbName + "." + rel.getRelName() + " rdfs:domain rdb:" + dbName + "." + pyNametoZhName(rel.getFromTb()) + " .");
            triples.add("rdb:" + dbName + "." + rel.getRelName() + " rdfs:range meta:blankNode .");
            System.out.println("rdb:" + dbName + "." + rel.getRelName() + " rdfs:range meta:blankNode .");
            //多对多关联表非主键字段作为数据类型属性定义(也许也可以作为其他类型的属性,不过更为复杂,后面再说)
            Iterator<Column> cols = tbColsWithTypes.get(relTbName).iterator();
            while(cols.hasNext()){
                Column col = cols.next();  //得到当前字段
                triples.add("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdf:type owl:DatatypeProperty .");
                System.out.println("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdf:type owl:DatatypeProperty .");
                triples.add("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:label \"" + pyNametoZhName(col.getColName()) + "\" .");  //加入当前属性的可读名称
                System.out.println("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:label \"" + pyNametoZhName(col.getColName()) + "\" .");
                triples.add("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:domain meta:blankNode .");
                System.out.println("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:domain meta:blankNode .");
                triples.add("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:range " + dataTypeToStr(col.getDataTp()) + " .");
                System.out.println("rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " rdfs:range " + dataTypeToStr(col.getDataTp()) + " .");
            }
        }
        return triples;
    }


    /**
     * 多对多关联表模式层的转换
     * @return 结果triple的列表
     * @throws SQLException
     */
    public List<String> transRelationInstance() throws SQLException{
        LinkedList<String> triples = new LinkedList<>();
        Iterator<String> relTbNames = keyToRefTbMap.keySet().iterator();
        while(relTbNames.hasNext()){
            String relTbName = relTbNames.next();  //得到当前处理的多对多关联表名
            Relation rel = relTbToRelMap.get(relTbName);  //得到该多对多关联表的描述信息
            ResultSet rs = dbReader.queryAll(relTbName);  //查询该多对多关联表的所有记录
            while(rs.next()){  //对于查询到的每行结果
                String uuid = getUUID();
                triples.add("rdb:" + uuid + " rdf:type meta:blankNode .");
                System.out.println("rdb:" + uuid + " rdf:type meta:blankNode .");
                String sbj = "rdb:" + dbName + "." + pyNametoZhName(rel.getFromTb()) + "." + rs.getInt(keyToRefTbMap.get(relTbName).get(rel.getFromTb()));
                String obj = "rdb:" + dbName + "." + pyNametoZhName(rel.getToTb()) + "." + rs.getInt(keyToRefTbMap.get(relTbName).get(rel.getToTb()));
                triples.add(sbj + " rdb:" + dbName + "." + rel.getRelName() + " rdb:" + uuid + " .");
                System.out.println(sbj + " rdb:" + dbName + "." + rel.getRelName() + " rdb:" + uuid + " .");
                triples.add("rdb:" + uuid + " meta:实例 " + obj + " .");
                System.out.println("rdb:" + uuid + " meta:实例 " + obj + " .");
                //非主键字段值转属性值
                Iterator<Column> cols = tbColsWithTypes.get(relTbName).iterator();
                while(cols.hasNext()){
                    Column col = cols.next();
                    String tmpStr = "rdb:" + uuid + " rdb:" + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()) + " ";
                    switch(col.getDataTp()){  //根据字段值的数据类型做相应操作
                        case STR:tmpStr += "\"" + rs.getString(col.getColName()) + "\"^^xsd:string .";break;
                        case INT:tmpStr += rs.getInt(col.getColName()) + "^^xsd:integer .";break;
                        case DBL:tmpStr += rs.getDouble(col.getColName()) + "^^xsd:decimal .";break;
                        case DT:tmpStr += rs.getDate(col.getColName()) + "^^xsd:date .";break;
                        case TM:tmpStr += rs.getTime(col.getColName()) + "^^xsd:time .";break;
                        case DTTM:tmpStr += rs.getString(col.getColName()) + "^^xsd:dateTime .";break;
                    }
                    triples.add(tmpStr);
                    System.out.println(tmpStr);
                }
            }
        }
        return triples;
    }


    /**
     * 数据类型到以"xsd"为前缀的字符串的映射
     * @param dt 数据类型
     * @return 以"xsd"为前缀的字符串
     */
    private String dataTypeToStr(DataType dt){
        switch (dt){
            case INT:return "xsd:integer";
            case STR:return "xsd:string";
            case DBL:return "xsd:decimal";
            case DT:return "xsd:date";
            case TM:return "xsd:time";
            case DTTM:return "xsd:dateTime";
        }
        return "xsd:string";  //默认是字符串
    }

    /**
     * 将拼音名称映射为中文名称
     * @param pyName 拼音名称
     * @return 中文名称
     */
    private String pyNametoZhName(String pyName){
        return pyToZh.containsKey(pyName) ? pyToZh.get(pyName) : pyName;
    }

    /**
     * UUID生成器
     * @return 一个UUID字符串
     */
    private String getUUID(){
        String uuid = UUID.randomUUID().toString().replace("-","");
        return uuid;
    }

    /**
     * 将从数据库中读取出的有关处理多对多关联表转换的信息分割开,分割的依据是 'isPri' 列的取值
     * @throws SQLException
     */
    private void DivideRelInfo() throws SQLException{
        ResultSet rs = dbReader.schemaInfoOfRelTb(relTbToRelMap);
        while(rs.next()){
            String tbName = rs.getString("table_name");
            String colName = rs.getString("column_name");
            String refTbName = rs.getString("referenced_table_name");
            if(rs.getInt("isPri") == 1){  //该条记录描述了多对多关联表的主键和其引用表的表名
                if(keyToRefTbMap.containsKey(tbName)){
                    keyToRefTbMap.get(tbName).put(refTbName,colName);
                }else{
                    HashMap<String,String> pair = new HashMap<>();
                    pair.put(refTbName,colName);  //实例关系由from方指向to方,因此键是引用表名,值是列名
                    keyToRefTbMap.put(tbName,pair);
                }
            }else{  //该条记录描述了多对多关联表的非主键字段及其值的数据类型
                if(tbColsWithTypes.containsKey(tbName)){
                    tbColsWithTypes.get(tbName).add(new Column(colName,PropertyType.DP,dbReader.dataTpMaps(refTbName),null));
                }else{
                    LinkedList<Column> colList = new LinkedList<>();
                    colList.add(new Column(colName,PropertyType.DP,dbReader.dataTpMaps(refTbName),null));
                    tbColsWithTypes.put(tbName,colList);
                }
            }
        }
    }
}
