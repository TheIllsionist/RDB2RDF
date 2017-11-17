import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

/**
 * Created by The Illsionist on 2017/11/17.
 */
public class JenaTest {
    static String bookURI = "http://kse.seu.edu.cn/rdb#图书.书籍.1";
    static String fullName = "Java多线程编程核心技术";

    public static void main(String args[]){
        Model model = ModelFactory.createDefaultModel();
        Resource bookClass = model.createResource("http://kse.seu.edu.cn/rdb#图书.书籍");
        bookClass.addProperty(RDF.type,OWL.Class);
        Resource book1 = model.createResource(bookURI);
        book1.addProperty(RDF.type, bookClass);
        Property tmpPro = model.createProperty("http://kse.seu.edu.cn/rdb#图书.书籍.名称");
        tmpPro.addProperty(RDFS.domain,bookClass);
        tmpPro.addProperty(RDFS.range, XSD.xstring);
        book1.addProperty(tmpPro,"Java多线程编程核心技术");
        model.write(System.out,"N-TRIPLES");  //格式就是 N-Triples,定了
    }
}
