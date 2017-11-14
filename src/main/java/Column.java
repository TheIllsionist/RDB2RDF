/**
 * Created by The Illsionist on 2017/11/9.
 */
public class Column {

    private String colName = null;  //字段名
    private PropertyType rdfTp = null;  //将字段转换为rdf时该字段被转换为哪种属性(Datatype Property还是Object Property)
    private DataType dataTp = null;  //字段值的数据类型
    private Object value = null; //默认值为null

    public Column(String colName, DataType dataTp){
        this.colName = colName;
        this.dataTp = dataTp;
    }

    public Column(String colName,DataType dataTp,Object value){
        this.colName = colName;
        this.dataTp = dataTp;
        this.value = value;
    }

    public Column(String colName, PropertyType rdfTp, DataType dataTp, Object value){
        this.colName = colName;
        this.rdfTp = rdfTp;
        this.dataTp = dataTp;
        this.value = value;
    }

    public String getColName() {
        return colName;
    }

    public void setColName(String colName) {
        this.colName = colName;
    }

    public PropertyType getRdfTp() {
        return rdfTp;
    }

    public void setRdfTp(PropertyType rdfTp) {
        this.rdfTp = rdfTp;
    }

    public DataType getDataTp() {
        return dataTp;
    }

    public void setDataTp(DataType dataTp) {
        this.dataTp = dataTp;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
