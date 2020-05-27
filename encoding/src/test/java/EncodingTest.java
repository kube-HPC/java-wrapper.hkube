import hkube.encoding.IEncoder;
import hkube.encoding.MSGPackEncoder;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EncodingTest {
    @Test
    public void testEncodeDecode(){
        IEncoder encoder = new MSGPackEncoder();
        HashMap rootMap = new HashMap();
        Map secondLevelMap = new HashMap();
        rootMap.put("field1",5);
        secondLevelMap.put("field2","value2");
        rootMap.put("field3",secondLevelMap);
        JSONObject jsonObject = new JSONObject(rootMap);
        byte[] mybytes = encoder.encode(rootMap);
        Map decodedObj = encoder.decode(mybytes);
        System.out.println(decodedObj.get("field1"));
    }
}
