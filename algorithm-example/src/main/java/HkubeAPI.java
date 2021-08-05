import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HkubeAPI implements IAlgorithm {


    @Override
    public void Init(Map args) {

    }

    @Override
    public Map Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("myAnswer", 33);
        data.put("mirror", input);
        ArrayList jsonArray = new ArrayList();
        jsonArray.add(data);
//        Map result = hkubeAPI.startAlgorithm("green-alg", jsonArray, false);
        Map simpleInput = new HashMap();
//        Map files = new HashMap();
//        files.put("link", "thislink");
//        files.put("other", "otherValue");
//        simpleInput.put("arraySize", 100);
//        simpleInput.put("bufferSize", 1500);



//        Map stroedResult = hkubeAPI.startStoredPipeLine("bug", simpleInput);
        List ll = new ArrayList();
        ll.add("shash");
        ll.add("ff");
//        Map stroedResult = hkubeAPI.startAlgorithm("green-alg",ll,false);


//        final String nodeName = "node1";
//        final String apiServerDownloadUrl = "http://63.34.172.241/hkube/api-server/api/v1/storage/download/custom/";
//        String path = (String) ((Map) ((Map) ((ArrayList) stroedResult.get("response")).stream().filter(map -> ((Map) map).get("nodeName").equals(nodeName)).findFirst().get()).get("info")).get("path");
//        String encodedrul = URLEncoder.encode(path, "UTF-8");
//        encodedrul = apiServerDownloadUrl + encodedrul;
//        URL url = new URL(encodedrul);
//        InputStream is = url.openConnection().getInputStream();
//        byte[] bytes = new byte[4096];
//        BufferedReader br = new BufferedReader(new InputStreamReader(is));
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//        is.read(new byte[6]);
//        int nRead;
//        while ((nRead = is.read(bytes, 0, bytes.length)) != -1) {
//            buffer.write(bytes, 0, nRead);
//        }
//        byte[] outputAsBytes = buffer.toByteArray();
//        String str = new String(outputAsBytes);
//


        INode node = new INode()
        {
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
                return "green-alg";
            }

            @Override
            public void setAlgorithmName(String algorithmName) {

            }
        };
        INode[] nodes = {node};
//        Map raw = hkubeAPI.startRawSubPipeLine("myRaw", nodes, new HashMap(), new HashMap(), new HashMap());
//        List raw = (List)hkubeAPI.startStoredPipeLine("simple",new HashMap(),true);
        Map algResult = new HashMap<>();
        System.out.println("PrintSomething");
//        algResult.put("storedResult", stroedResult);
//        algResult.put("algo-green-result", result);
//        algResult.put("rawResult", raw);
        hkubeAPI.startSpan("a start",new HashMap());
        hkubeAPI.startSpan("b start",new HashMap());
        hkubeAPI.finishSpan(new HashMap());
        hkubeAPI.finishSpan(new HashMap());

        return input;
//        return stroedResult;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
