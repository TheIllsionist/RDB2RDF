import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
        String xsd = "http://www.w3.org/2001/XMLSchema#";
        String meta = "http://kse.seu.edu.cn/meta#";
        String wgbq = "http://kse.seu.edu.cn/wgbq#";
        model.setNsPrefix("rdb",rdb);
        model.setNsPrefix("rdf",rdf);
        model.setNsPrefix("rdfs",rdfs);
        model.setNsPrefix("owl",owl);
        model.setNsPrefix("meta",meta);
        model.setNsPrefix("wgbq",wgbq);
        Resource blankNode = model.createResource(meta + "blankNode").addProperty(RDF.type,OWL.Class);
        model.createProperty(meta + "实例").addProperty(RDF.type,OWL.ObjectProperty).addProperty(RDFS.domain,blankNode);
        Property pic = model.createProperty(meta + "pic");
        pic.addProperty(RDF.type,OWL.DatatypeProperty).addProperty(RDFS.label,"图片");
        //数据写文件测试
        try{
            File initGraph = new File("./src/main/resources/initGraph.owl");
            FileOutputStream outputStream = new FileOutputStream(initGraph,false);
            RDFDataMgr.write(outputStream,model,Lang.TURTLE); // 将模型灌到stream输出流中
            outputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        //数据读文件测试
        InputStream in = FileManager.get().open("./src/main/resources/initGraph.owl");
        if(in == null){
            System.out.println("未找到文件!");
            return;
        }else{
            Model newModel = ModelFactory.createDefaultModel();
            newModel.read(in,null,"turtle");
            model.write(System.out,"turtle");
        }
    }

}
