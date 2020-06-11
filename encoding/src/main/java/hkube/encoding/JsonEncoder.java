package hkube.encoding;

import org.json.JSONObject;

import java.util.Map;

public class JsonEncoder extends BaseEncoder implements IEncoder {
    @Override
    public byte[] encode(Object obj) {
        JSONObject jsonObject;
        if (obj instanceof Map) {
            jsonObject = new JSONObject((Map) obj);
            return jsonObject.toString().getBytes();
        } else if (obj instanceof JSONObject) {
            return obj.toString().getBytes();
        } else
            return new byte[0];
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        return encode(obj);
    }

    @Override
    public Object decode(byte[] data) {
        JSONObject jsonObject = new JSONObject(new String(data));
        return jsonObject;
    }

    @Override
    public Object decodeNoHeader(byte[] data) {
        return null;
    }

    @Override
    public Integer getEncodingType() {
        return 2;
    }
}
