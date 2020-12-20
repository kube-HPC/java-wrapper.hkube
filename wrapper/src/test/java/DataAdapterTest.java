import hkube.algo.wrapper.DataAdapter;
import hkube.algo.wrapper.WrapperConfig;
import hkube.communication.ICommConfig;
import hkube.communication.IRequest;
import hkube.communication.IRequestFactory;
import hkube.model.HeaderContentPair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataAdapterTest {
    @Test
    public void testMetaData() {
        Map data = new HashMap();
        data.put("first", "String");
        data.put("second", 8);
        data.put("third", new byte[9]);
        List myList = new ArrayList();
        myList.add("one");
        myList.add("tow");
        data.put("forth", myList);
        List savePaths = new ArrayList();
        savePaths.add("nodeName.first");
        savePaths.add("nodeName.second");
        savePaths.add("nodeName.third");
        savePaths.add("nodeName.forth");
        savePaths.add("nodeName.forth.1");

        Map metaData = DataAdapter.getMetadata(savePaths, data);
        assert ((Map) metaData.get("nodeName.first")).get("type").equals("string");
        assert ((Map) metaData.get("nodeName.second")).get("type").equals("number");
        assert ((Map) metaData.get("nodeName.third")).get("type").equals("bytearray");
        assert ((Map) metaData.get("nodeName.forth")).get("type").equals("array");
        assert ((Map) metaData.get("nodeName.forth")).get("size").equals(2);
        assert ((Map) metaData.get("nodeName.forth.1")).get("type").equals("string");
        savePaths = new ArrayList();
        savePaths.add("nodeName");
        metaData = DataAdapter.getMetadata(savePaths, new byte[4]);
        assert ((Map) metaData.get("nodeName")).get("type").equals("bytearray");
    }


}
