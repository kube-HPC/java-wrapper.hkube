import hkube.encoding.IEncoder;
import hkube.encoding.MSGPackEncoder;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        rootMap.put("field4","Hello".getBytes());
        byte[] mybytes = encoder.encode(rootMap);
        Map decodedObj = (Map)encoder.decode(mybytes);
        assert decodedObj.get("field1").equals(5);
        assert decodedObj.get("field4").getClass().equals(byte[].class);
        assert new String((byte[])decodedObj.get("field4")).equals("Hello");
    }
}
