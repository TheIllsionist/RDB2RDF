import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.io.*;

/**
 * Created by The Illsionist on 2017/11/17.
 */
public class JenaTest {

    public static void main(String args[]){
        Model model = ModelFactory.createDefaultModel();
        String rdb = "http://kse.seu.edu.cn/rdb#";
        String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
        String owl = "http://www.w3.org/2002/07/owl#";
        model.setNsPrefix("rdb",rdb);
        model.setNsPrefix("rdf",rdf);
        model.setNsPrefix("rdfs",rdfs);
        model.setNsPrefix("owl",owl);
        model.createResource(rdb + "图书.书籍.名称").addProperty(RDF.type,OWL.DatatypeProperty);
        Resource bookClass = model.createResource(rdb + "图书.书籍");
        bookClass.addProperty(RDF.type, OWL.Class);
        Resource book = model.createResource(rdb + "图书.书籍.1");
        Resource book2 = model.createResource(rdb + "图书.书籍.1");  //Jena不会重复创建Resource
        book.addProperty(RDF.type,bookClass);
        try{
            //使用Jena将内存中的模型写入文件
            File testDB = new File("./src/main/resources/KG/TestDB.owl");
            FileOutputStream stream = new FileOutputStream(testDB,false);
            RDFDataMgr.write(stream,model,Lang.NTRIPLES);
            stream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
