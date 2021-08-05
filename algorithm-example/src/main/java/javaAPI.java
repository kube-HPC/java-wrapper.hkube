
import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class javaAPI implements IAlgorithm {

    @Override
    public void Init(Map args) {

    }


    @Override
    public Object Start(Map args, IHKubeAPI hkubeAPI) throws Exception {

        String nodeName = (String) args.get("nodeName");

        Collection<Object> input = (Collection<Object>)args.get("input");
        Map action = (Map) input.iterator().next();

        String act = (String)action.get("action");

        if(act.equals("startAlg")){
            //{"action":"startAlg","algName":"green-alg","alginput":[]}
            String algName = (String) action.get("algName");
            List<String> listSource = (List<String>) action.get("alginput");
            Object jnk =  hkubeAPI.startAlgorithm(algName,listSource ,true);
            return jnk;
        }

        if(act.equals("startAlgBinary")){
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream("Chameleon.jpg");
            BufferedImage bImage = ImageIO.read(stream);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "jpg", bos);
            byte[] data = bos.toByteArray();
            //{"action":"startAlg","algName":"green-alg","alginput":[]}
            String algName = (String) action.get("algName");
            List<byte[]> list = Arrays.asList(data);
            Object jnk =  hkubeAPI.startAlgorithm(algName,list ,true);
            return jnk;
        }

        if(act.equals("startStored")){
            //{"action":"startStored","PipeName":"simple","PipeInput":[]}
            String PipeName = (String) action.get("PipeName");
            List<String> listSource = (List<String>) action.get("PipeInput");
            Object jnk =   hkubeAPI.startStoredPipeLine(PipeName,new HashMap());
            return jnk;
        }




        INode node = new INode() {
            @Override
            public String getName() {
                return "yellow-alg-NOde";
            }

            @Override
            public JSONObject[] getInput() {
                return new JSONObject[0];
            }

            @Override
            public void setInput(JSONObject[] input) {

            }
            @Override
            public String getAlgorithmName() {
                return "yellow-alg";
            }

            @Override
            public void setAlgorithmName(String algorithmName) {

            }
        };

        if(act.equals("startRaw")){
            //{"action":"startRaw","PipeName":"simple","PipeInput":{}}
            String PipeName = (String) action.get("PipeName");

            //List<String> listSource = (List<String>) action.get("PipeInput");
            //String name, INode[] nodes, Map flowInput, Map options, Map webhooks
            INode jnkNode = createNode("jnk","green-alg" , "42");
            INode[] nodes ={jnkNode};
            Object jnk =   hkubeAPI.startRawSubPipeLine(PipeName,nodes,new HashMap(),new HashMap(),new HashMap());
            return jnk;
        }

        return  42;
    }






    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }


    private INode createNode(String nodeName,String algName,String nodeInput) {
        JSONObject obj=new JSONObject();
        obj.put("input",nodeInput);
        JSONObject[] obj1 =new JSONObject[1];
        obj1[0]=obj;

        INode node = new INode() {
            @Override
            public String getName() {
                return nodeName;
            }

            @Override
            public JSONObject[] getInput() {
                return obj1;
            }

            @Override
            public void setInput(JSONObject[] input) {

            }

            @Override
            public String getAlgorithmName() {
                return algName;
            }

            @Override
            public void setAlgorithmName(String algName) {

            }

        };
        return node;
    }
}