/**
 * Created by The Illsionist on 2017/11/10.
 */
public class Relation {

    private String fromTb = null;  //关系的from方表名
    private String toTb = null;  //关系的to方表名
    private boolean nTonN= true;  //是否是多对多关系
    private String relName = null;  //关系名

    public Relation(String relName,String fromTb,String toTb,boolean nToN){
        this.relName = relName;
        this.fromTb = fromTb;
        this.toTb = toTb;
        this.nTonN = nToN;
    }


    public String getFromTb() {
        return fromTb;
    }

    public void setFromTb(String fromTb) {
        this.fromTb = fromTb;
    }

    public String getToTb() {
        return toTb;
    }

    public void setToTb(String toTb) {
        this.toTb = toTb;
    }

    public boolean isnTonN() {
        return nTonN;
    }

    public void setnTonN(boolean nTonN) {
        this.nTonN = nTonN;
    }

    public String getRelName() {
        return relName;
    }

    public void setRelName(String relName) {
        this.relName = relName;
    }

}
