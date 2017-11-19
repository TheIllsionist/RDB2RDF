import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
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

    //模型构建信息
    private Model model = null;
    private String rdb = "http://kse.seu.edu.cn/rdb#";
    private String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
    private String owl = "http://www.w3.org/2002/07/owl#";
    private String meta = "http:kse.seu.edu.cn/meta#";
    private String xsd = "http://www.w3.org/2001/XMLSchema#";
    private Resource blankNode = null;
    private Property instanceIs = null;
    private Property pic = null;

    //临时存储
    /**
     * 在进行单主键实体表的处理时存储所有单主键实体表的表名和它们拥有的字段名和字段的数据类型
     * 在进行多对多关联表的处理时存储所有多对多关联表的表名和它们拥有的非主键字段以及数据类型
     */
    private HashMap<String,List<Column>> tbColsWithTypes = null;
    private HashMap<String,HashMap<String,String>> keyToRefTbMap = null;  //多对多关联表主键与该主键所引用的表名的对应关系

    /**
     * 模型转换器构造函数,构造时创建数据库信息读取器
     * @param dbName 数据库名
     */
    public ModelTransformer(String dbName){
        this.dbName = dbName;
        this.dbReader = new DBReader(dbName);
        keyToRefTbMap = new HashMap<>();
        initModel();
    }

    /**
     * 根据元本体构建规范对模型进行初始化
     */
    private void initModel(){
        //创建模型同时定义前缀
        model = ModelFactory.createDefaultModel();
        model.setNsPrefix("rdb",rdb);
        model.setNsPrefix("rdf",rdf);
        model.setNsPrefix("rdfs",rdfs);
        model.setNsPrefix("owl",owl);
        model.setNsPrefix("meta",meta);
        model.setNsPrefix("xsd",xsd);
        blankNode = model.createResource(meta + "blankNode");
        blankNode.addProperty(RDF.type,OWL.Class);
        instanceIs = model.createProperty(meta + "实例");
        instanceIs.addProperty(RDF.type,OWL.ObjectProperty).addProperty(RDFS.domain,blankNode);
        pic = model.createProperty(meta + "pic");
        pic.addProperty(RDF.type,OWL.DatatypeProperty).addProperty(RDFS.label,"图片");
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

    public Model getModel(){
        return this.model;
    }

    /**
     * 单主键实体表模式层的转换
     * @return 结果triple的列表
     * @throws SQLException
     */
    public void transEntitySchema() throws SQLException{
        tbColsWithTypes = dbReader.tbColsWithTypes(entityTableMap); //读取每张单主键实体表的所有字段和对应数据类型于内存中
        Iterator<String> tableIterator = tbColsWithTypes.keySet().iterator();
        while(tableIterator.hasNext()){
            String tbName = tableIterator.next();
            Resource tbClass = model.createResource(rdb + dbName + "." + pyNametoZhName(tbName))
                    .addProperty(RDF.type, OWL.Class).addProperty(RDFS.label,pyNametoZhName(tbName)); //表名为类名,添加label
            List<Column> cols = tbColsWithTypes.get(tbName);
            for(Column col : cols){
                PropertyType proType = colJudgeDic.containsKey(col.getColName())?colJudgeDic.get(col.getColName()):PropertyType.DP;
                col.setRdfTp(proType);           //记录当前字段的rdfType避免重复计算(很重要)
                if(proType == PropertyType.DP){  //字段作为数据类型属性
                    model.createProperty(rdb + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()))
                            .addProperty(RDF.type,OWL.DatatypeProperty).addProperty(RDFS.label,pyNametoZhName(col.getColName()))
                            .addProperty(RDFS.domain,tbClass).addProperty(RDFS.range,dataTypeToXSD(col.getDataTp()));
                }else if(proType == PropertyType.OP){  //将字段作为对象属性
                    model.createProperty(rdb + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()))
                            .addProperty(RDF.type,OWL.ObjectProperty).addProperty(RDFS.label,pyNametoZhName(col.getColName()))
                            .addProperty(RDFS.range,blankNode);  //可能有多种类的实例有该对象属性,所以只设置range
                }else if(proType == PropertyType.GC){  //该字段描述了类型信息

                }else if(proType == PropertyType.GP){ //该字段表示其值需要被转换为图片
					//图片属性暂时统一使用 meta:pic
                }
            }
        }
    }

    /**
     * 转换单主键实体表的行为实例层
     * @return triple的列表
     * @throws SQLException
     */
    public void transEntityInstance() throws SQLException{
        Iterator<String> tableIterator = tbColsWithTypes.keySet().iterator();
        while(tableIterator.hasNext()){
            String tbName = tableIterator.next();
            Resource tbClass = model.getResource(rdb + dbName + "." + pyNametoZhName(tbName));//从Model中查询出该类
            ResultSet rs = dbReader.queryAll(tbName);  //查询该表所有行
            List<Column> cols = tbColsWithTypes.get(tbName); //得到该表的所有列
            while(rs.next()){  //对每行查询结果处理
                Resource sbj = model.createResource(rdb + dbName + "." + pyNametoZhName(tbName) + "." + rs.getInt(entityTableMap.get(tbName)));
                sbj.addProperty(RDF.type,tbClass); //定义主语是类的实例
				//该单主键实体表有描述列且当前实例描述列的值不为空或空串
				if(entityDescMap.get(tbName) != null && rs.getString(entityDescMap.get(tbName)) != null && !rs.getString(entityDescMap.get(tbName)).matches("\\s*")){
                    sbj.addProperty(RDFS.label,rs.getString(entityDescMap.get(tbName))); //给当前实例加上label
				}
                for(Column col:cols){
                    if(rs.getString(col.getColName()) == null || rs.getString(col.getColName()).matches("\\s*") || rs.wasNull())
                        continue;   //字段值为NULL或空串时跳过该字段
                    Property predicate = model.getProperty(rdb + dbName + "." + pyNametoZhName(tbName) + "." + pyNametoZhName(col.getColName()));//从Model中查出谓词
                    if(col.getRdfTp() == PropertyType.DP){
                        switch(col.getDataTp()){
                            case STR:sbj.addLiteral(predicate,model.createTypedLiteral(rs.getString(col.getColName()),XSDDatatype.XSDstring));break;
                            case INT:sbj.addLiteral(predicate,model.createTypedLiteral(rs.getInt(col.getColName()),XSDDatatype.XSDinteger));break;
                            case DBL:sbj.addLiteral(predicate,model.createTypedLiteral(rs.getDouble(col.getColName()),XSDDatatype.XSDdouble));break;
                            case DT:sbj.addLiteral(predicate,model.createTypedLiteral(rs.getDate(col.getColName()),XSDDatatype.XSDdate));break;
                            case TM:sbj.addLiteral(predicate,model.createTypedLiteral(rs.getTime(col.getColName()),XSDDatatype.XSDtime));break;
                            case DTTM:sbj.addLiteral(predicate,model.createTypedLiteral(rs.getString(col.getColName()),XSDDatatype.XSDdateTime));break;
                        }
                    }else if(col.getRdfTp() == PropertyType.OP){
                        Resource newOpClass = model.createResource(rdb + dbName + "." + pyNametoZhName(col.getColName()))
                                .addProperty(RDF.type,OWL.Class).addProperty(RDFS.label,pyNametoZhName(col.getColName())); //额外定义类同时添加label
                        Resource obj = model.createResource(rdb + dbName + "." + pyNametoZhName(col.getColName()) + "." + rs.getString(col.getColName()))
                                .addProperty(RDF.type,newOpClass).addProperty(RDFS.label,pyNametoZhName(rs.getString(col.getColName())));  //定义obj同时添加label
                        String uuid = getUUID();  //生成一个新的UUID码
                        Resource betweenNode = model.createResource(rdb + uuid).addProperty(RDF.type,blankNode);  //新增中介节点
                        sbj.addProperty(predicate,betweenNode); //sbj指向中介节点
                        betweenNode.addProperty(instanceIs,obj); //中介节点指向obj
                    }else if(col.getRdfTp() == PropertyType.GC){
                        String newClassName = rs.getString(col.getColName());
                        Resource newGcClass = null;
                        if(classIsContained(newClassName)){
                            newGcClass = model.getResource(rdb + dbName + "." + pyNametoZhName(rs.getString(col.getColName())));
                        }else {
                            newGcClass = model.createResource(rdb + dbName + "." + rs.getString(col.getColName()))
                                    .addProperty(RDF.type, OWL.Class).addProperty(RDFS.label, rs.getString(col.getColName()));  //额外定义类同时添加label
                        }
                        sbj.addProperty(RDF.type,newGcClass);  //当前实例也是该新类的实例
                    }else if(col.getRdfTp() == PropertyType.GP){  //该字段是图片字段
                        sbj.addProperty(pic,sbj.getLocalName().replaceAll("\\.","\\\\\\\\") + ".jpg");
                    }
                }
            }
        }
    }

    /**
     * 多对多关联表模式层的转换
     * @return 结果triple的列表
     * @throws SQLException
     */
    public void transRelationSchema() throws SQLException{
        if(tbColsWithTypes.size() != 0){  //复用 tbColsWithTypes 前先清空
            tbColsWithTypes.clear();
        }
        DivideRelInfo();  //查询并将结果分开
        Iterator<String> relTbNames = relTbToRelMap.keySet().iterator();
        while(relTbNames.hasNext()){  //遍历每一张多对多关系表
            String relTbName = relTbNames.next();  //当前多对多关系表的表名
            Relation rel = relTbToRelMap.get(relTbName);
            //多对多关联表名作为对象关系属性定义
            Property tbRelOp = model.createProperty(rdb + dbName + "." + rel.getRelName());
            tbRelOp.addProperty(RDF.type,OWL.ObjectProperty).addProperty(RDFS.label,rel.getRelName())
                    .addProperty(RDFS.domain,model.getResource(rdb + dbName + "." + pyNametoZhName(rel.getFromTb())))
                    .addProperty(RDFS.range,blankNode);  //设置类型,label,domain,range
            //多对多关联表非主键字段作为数据类型属性定义(也许也可以作为其他类型的属性,不过更为复杂,后面再说)
            Iterator<Column> cols = tbColsWithTypes.get(relTbName).iterator();
            while(cols.hasNext()){
                Column col = cols.next();
                Property colDp = model.createProperty(rdb + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()));
                colDp.addProperty(RDF.type,OWL.DatatypeProperty).addProperty(RDFS.label,pyNametoZhName(col.getColName()))
                        .addProperty(RDFS.domain,blankNode).addProperty(RDFS.range,dataTypeToXSD(col.getDataTp())); //添加属性类型,label,domain,range
            }
        }
    }


    /**
     * 多对多关联表实例层的转换
     * @return 结果triple的列表
     * @throws SQLException
     */
    public void transRelationInstance() throws SQLException{
        Iterator<String> relTbNames = keyToRefTbMap.keySet().iterator();
        while(relTbNames.hasNext()){
            String relTbName = relTbNames.next();
            Relation rel = relTbToRelMap.get(relTbName);
            Property predicate = model.getProperty(rdb + dbName + "." + rel.getRelName());  //查询model得到谓词
            ResultSet rs = dbReader.queryAll(relTbName);  //查询该多对多关联表的所有记录
            while(rs.next()){
                String uuid = getUUID();
                Resource betweenNode = model.createResource(rdb + uuid).addProperty(RDF.type,blankNode);  //创建中间节点
                //查询得到主语
                Resource sbj = model.getResource(rdb + dbName + "." + pyNametoZhName(rel.getFromTb()) + "." + rs.getInt(keyToRefTbMap.get(relTbName).get(rel.getFromTb())));
                //查询得到宾语
                Resource obj = model.getResource(rdb + dbName + "." + pyNametoZhName(rel.getToTb()) + "." + rs.getInt(keyToRefTbMap.get(relTbName).get(rel.getToTb())));
                sbj.addProperty(predicate,betweenNode); //主语指向中间节点
                betweenNode.addProperty(instanceIs,obj);  //中间节点指向宾语
                //非主键字段值转属性值
                Iterator<Column> cols = tbColsWithTypes.get(relTbName).iterator();
                while(cols.hasNext()){
                    Column col = cols.next();
                    Property colDp = model.getProperty(rdb + dbName + "." + pyNametoZhName(relTbName) + "." + pyNametoZhName(col.getColName()));
                    switch(col.getDataTp()){  //根据字段值的数据类型做相应操作
                        case STR:betweenNode.addLiteral(colDp,model.createTypedLiteral(rs.getString(col.getColName()),XSDDatatype.XSDstring));break;
                        case INT:betweenNode.addLiteral(colDp,model.createTypedLiteral(rs.getInt(col.getColName()),XSDDatatype.XSDinteger));break;
                        case DBL:betweenNode.addLiteral(colDp,model.createTypedLiteral(rs.getDouble(col.getColName()),XSDDatatype.XSDdouble));break;
                        case DT:betweenNode.addLiteral(colDp,model.createTypedLiteral(rs.getDate(col.getColName()),XSDDatatype.XSDdate));break;
                        case TM:betweenNode.addLiteral(colDp,model.createTypedLiteral(rs.getDate(col.getColName()),XSDDatatype.XSDtime));break;
                        case DTTM:betweenNode.addLiteral(colDp,model.createTypedLiteral(rs.getString(col.getColName()),XSDDatatype.XSDdateTime));break;
                    }
                }
            }
        }
    }


    /**
     * 数据类型到以"xsd"为前缀的字符串的映射
     * @param dt 数据类型
     * @return 以"xsd"为前缀的字符串
     */
    private Resource dataTypeToXSD(DataType dt){
        switch (dt){
            case INT:return XSD.integer;
            case STR:return XSD.xstring;
            case DBL:return XSD.decimal;
            case DT:return XSD.date;
            case TM:return XSD.time;
            case DTTM:return XSD.dateTime;
        }
        return XSD.xstring;  //默认是字符串
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

    /**
     * 检查数据库中单主键实体表名转换成的那些类名是否已经包含了这个新类名
     * 无论新类名是以zh还是en的形式出现,都能检查出来
     * @param name 新类名
     * @return 包含则返回true,不包含则返回false
     */
    private boolean classIsContained(String name){
        boolean isContained = false;
        Iterator<String> oriClasses = entityTableMap.keySet().iterator();
        while(oriClasses.hasNext()){
            String oriClassName = oriClasses.next();
            if(oriClassName.equals(name) || pyNametoZhName(oriClassName).equals(name)){
                isContained = true;
                break;
            }
        }
        return isContained;
    }
}
