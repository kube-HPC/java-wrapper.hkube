package hkube.encoding;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public interface IEncoder {
    public byte[] encode(Map obj);
    public Map decode(byte[] data);
}
