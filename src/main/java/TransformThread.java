import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by The Illsionist on 2017/11/10.
 */
public class TransformThread implements Runnable{

    private String dbName = null;  //数据库名
    private ModelTransformer modelTransformer = null;  //转换器

    public TransformThread(String dbName){
        this.dbName = dbName;
        modelTransformer = new ModelTransformer(dbName);
    }

    @Override
    public void run() {
        if(dbName == null)
            return;
        //读取模型转换所必须的人工配置信息
        Thread subReadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                modelTransformer.setEntityTableMap(getEntityTableMap());
                modelTransformer.setPyToZh(getPyToZh());
                modelTransformer.setColJudgeDic(getColJudgeDic());
                modelTransformer.setRelTbToRelMap(getRelTbToRelMap());
                modelTransformer.setEntityDescMap(getEntityDescMap());
            }
        });
        System.out.println("正读取数据库" + dbName + "的转换配置信息......");
        subReadThread.start();
        try {
            subReadThread.join();
        } catch (InterruptedException e) {
            System.out.println("执行读取数据库" + dbName + "转换配置信息子线程时出现错误!");
            e.printStackTrace();
        }
        System.out.println("数据库" + dbName + "的转换配置信息读取成功!");
        //进行模型转换
        try {
            System.out.println("单主键实体模式层:");
            System.out.println("----------------------------------------------------------------------------------");
            modelTransformer.transEntitySchema();
            System.out.println("单主键实体实例层:");
            System.out.println("----------------------------------------------------------------------------------");
            modelTransformer.transEntityInstance();
            System.out.println("多对多实体模式层");
            System.out.println("----------------------------------------------------------------------------------");
            modelTransformer.transRelationSchema();
            System.out.println("多对多实体实例层:");
            System.out.println("----------------------------------------------------------------------------------");
            modelTransformer.transRelationInstance();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取中英文映射词典文件
     * 目前文件路径是写死在程序里面的,后期如果有机会可以考虑修改成配置文件等等
     */
    private HashMap<String,String> getPyToZh(){
        BufferedReader reader = null;
        HashMap<String,String> pyToZh = null;
        try {
            reader = new BufferedReader(new FileReader(".\\src\\main\\resources\\" + dbName + "\\pyToZh.txt"));
            pyToZh = new HashMap<>();
            String line = reader.readLine();
            while(line != null){
                String[] lineArray = line.trim().split("\\s+");
                pyToZh.put(lineArray[0],lineArray[1]);
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到数据库" + dbName + "的pyToZh.txt文件");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("在读取数据库" + dbName + "的pyToZh.txt文件时发生错误!");
            e.printStackTrace();
            return null;
        }
        return pyToZh;
    }

    /**
     * 读取字段分类字典
     * 目前文件路径是写死在程序里面的,后期如果有机会可以考虑修改成配置文件等等
     */
    private HashMap<String,PropertyType> getColJudgeDic(){
        BufferedReader reader = null;
        HashMap<String,PropertyType> colJudgeDic = null;
        try{
            reader = new BufferedReader(new FileReader(".\\src\\main\\resources\\" + dbName + "\\colJudgeDic.txt"));
            colJudgeDic = new HashMap<>();
            String line = reader.readLine();
            while(line != null){
                String[] lineArray = line.trim().split("\\s+");
                colJudgeDic.put(lineArray[0],lineArray[1].equals("OP") ? PropertyType.OP : PropertyType.GC);
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到数据库" + dbName + "的colJudgeDic.txt文件");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("在读取数据库" + dbName + "的colJudgeDic.txt文件时发生错误!");
            e.printStackTrace();
            return null;
        }
        return colJudgeDic;
    }

    /**
     * 读取多对多关联表或者存在多对一情况的表到关系的映射
     * 目前文件路径是写死在程序里面的,后期如果有机会可以考虑修改成配置文件等等
     */
    private HashMap<String,Relation> getRelTbToRelMap(){
        BufferedReader reader = null;
        HashMap<String,Relation> relTbToRelMap = null;
        try{
            reader = new BufferedReader(new FileReader(".\\src\\main\\resources\\" + dbName + "\\relTbToRelMap.txt"));
            relTbToRelMap = new HashMap<>();
            String line = reader.readLine();
            while(line != null){
                String[] lineArray = line.trim().split(":");
                boolean nTonN = lineArray[1].equals("n-n") ? true : false;
                if(nTonN){
                    String[] details = lineArray[2].split("-");
                    relTbToRelMap.put(lineArray[0],new Relation(details[1],details[0],details[2],nTonN));
                }else{
                    //此处添加处理多对一的情况
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到数据库" + dbName + "的relTbToRelMap.txt文件");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("在读取数据库" + dbName + "的relTbToRelMap.txt文件时发生错误!");
            e.printStackTrace();
            return null;
        }
        return relTbToRelMap;
    }

    /**
     * 得到数据库中所有的单主键实体表的表名和主键名映射
     * @return 单主键实体表表名与其主键名的映射
     */
    private HashMap<String,String> getEntityTableMap(){
        BufferedReader reader = null;
        HashMap<String,String> entityTableMap = null;
        try {
            reader = new BufferedReader(new FileReader(".\\src\\main\\resources\\" + dbName + "\\entityTableMap.txt"));
            entityTableMap = new HashMap<>();
            String line = reader.readLine();
            while(line != null){
                String[] lineArray = line.trim().split(":");
                entityTableMap.put(lineArray[0],lineArray[1]);  //表名为键,主键名为值
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到数据库" + dbName + "的entityTableMap.txt文件");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("在读取数据库" + dbName + "的entityTableMap.txt文件时发生错误!");
            e.printStackTrace();
            return null;
        }
        return entityTableMap;
    }

    /**
     * 得到数据库中所有单主键实体表的表名和描述表中实例可读名称的列名
     * @return 单主键实体表名和描述表名实例可读名称的列名映射
     */
    private HashMap<String,String> getEntityDescMap(){
        BufferedReader reader = null;
        HashMap<String,String> entityDescMap = null;
        try {
            reader = new BufferedReader(new FileReader(".\\src\\main\\resources\\" + dbName + "\\entityDescMap.txt"));
            entityDescMap = new HashMap<>();
            String line = reader.readLine();
            while(line != null){
                String[] lineArray = line.trim().split(":");
                entityDescMap.put(lineArray[0],lineArray[1]);  //表名为键,描述列名为值
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("找不到数据库" + dbName + "的entityDescMap.txt文件");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("在读取数据库" + dbName + "的entityDescMap.txt文件时发生错误!");
            e.printStackTrace();
            return null;
        }
        return entityDescMap;
    }
}
