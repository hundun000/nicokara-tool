package hundun.nicokaratool.cantonese;

import hundun.nicokaratool.base.BaseService.ServiceResult;

import java.io.*;

public class CantoneseRunner {


    static CantoneseService service = new CantoneseService();

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter name: ");
        String name = br.readLine();

        ServiceResult serviceResult = service.work(name);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/" + name +".out.txt", false))){
            writer.write(serviceResult.getKanji());
            writer.write("\n\n\n");
            writer.write(serviceResult.getRuby());
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }




}
