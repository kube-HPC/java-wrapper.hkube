package hkube.encoding;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public interface IEncoder {


    byte[] encode(Object obj);

    byte[] encodeNoHeader(Object obj);

    public Map decode(byte[] data);

    Map decodeNoHeader(byte[] data);
}
