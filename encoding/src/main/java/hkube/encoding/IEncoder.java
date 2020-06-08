package hkube.encoding;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public interface IEncoder {


    byte[] encode(Object obj);

    byte[] encodeNoHeader(Object obj);

    public Object decode(byte[] data);

    Object decodeNoHeader(byte[] data);
}
