import org.apache.jena.ontology.OntModel;
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
        OntModel model = ModelFactory.createOntologyModel();
        String ns = "http://kse.seu.edu.cn/rdb#";
        model.setNsPrefix("rdb",ns);
        Resource my = model.createResource(ns + "图书.书籍.1");
        my.addProperty(RDF.type, OWL.Class);
        try{
            File testDB = new File(".\\src\\main\\resources\\KG\\TestDB.rdf");
            FileOutputStream stream = new FileOutputStream(testDB);
            RDFDataMgr.write(stream,model,Lang.NTRIPLES);

            stream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
