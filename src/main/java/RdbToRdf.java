import java.util.Scanner;

/**
 * Created by The Illsionist on 2017/11/10.
 */
public class RdbToRdf {

    public static void main(String args[]){
        Scanner input = new Scanner(System.in);
        System.out.println("请输入要转换的数据库名称:");
        String dbName = input.nextLine();
        new Thread(new TransformThread(dbName)).start();  //启动线程
        input.close();
    }

}
