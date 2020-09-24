import hkube.encoding.BSONEncoder;
import hkube.encoding.IEncoder;
import hkube.encoding.JsonEncoder;
import hkube.encoding.MSGPackEncoder;
import org.junit.Test;


import java.util.HashMap;
import java.util.Map;

public class EncodingTest {
    @Test
    public void testEncodeDecodeMsgPack(){
        IEncoder encoder = new MSGPackEncoder();
        HashMap rootMap = new HashMap();
        Map secondLevelMap = new HashMap();
        rootMap.put("field1",5);
        secondLevelMap.put("field2","value2");
        rootMap.put("field3",secondLevelMap);
        rootMap.put("field4","Hello".getBytes());
        byte[] mybytes = encoder.encodeNoHeader(rootMap);
        Map decodedObj = (Map)encoder.decodeNoHeader(mybytes);
        assert decodedObj.get("field1").equals(5);
        assert decodedObj.get("field4") instanceof byte[];
        byte[] bytes = (byte[]) decodedObj.get("field4");
        assert new String(bytes).equals("Hello");
    }
    @Test
    public void testEncodeDecodeBSON(){
        IEncoder encoder = new BSONEncoder();
        HashMap rootMap = new HashMap();
        Map secondLevelMap = new HashMap();
        rootMap.put("field1",5);
        secondLevelMap.put("field2","value2");
        rootMap.put("field3",secondLevelMap);
        rootMap.put("field4","Hello".getBytes());
        byte[] mybytes = encoder.encodeNoHeader(rootMap);
        Map decodedObj = (Map)encoder.decodeNoHeader(mybytes);
        assert decodedObj.get("field1").equals(5);
        byte[] bytes = (byte[]) decodedObj.get("field4");
        assert new String(bytes).equals("Hello");
    }
    @Test
    public void testEncodeDecodeJSON(){
        IEncoder encoder = new JsonEncoder();
        HashMap rootMap = new HashMap();
        Map secondLevelMap = new HashMap();
        rootMap.put("field1",5);
        secondLevelMap.put("field2","value2");
        rootMap.put("field3",secondLevelMap);
        rootMap.put("field4","Hello".getBytes());
        byte[] mybytes = encoder.encodeNoHeader(rootMap);
        Map decodedObj = (Map)encoder.decodeNoHeader(mybytes);
        assert decodedObj.get("field1").equals(5);
    }
}
