package hkube.encoding;

import java.util.HashMap;
import java.util.Map;

public class EncodingManager extends BaseEncoder implements IEncoder {
    Map<String,IEncoder> encodings = new HashMap();
    String defaultEncoding;

    public EncodingManager(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
        IEncoder jsonEncoder = new JsonEncoder();
        encodings.put("2", jsonEncoder);
        encodings.put("json", jsonEncoder);
        IEncoder msgPackEncoder = new MSGPackEncoder();
        encodings.put("3", msgPackEncoder);
        encodings.put("msgpack", msgPackEncoder);

    }

    private Object decodeNoHeader(byte[] data, IEncoder encoder) {

        return encoder.decodeNoHeader(data);
    }

    @Override
    public byte[] encode(Object obj) {
        return  encodings.get(defaultEncoding).encode(obj);
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        return  encodings.get(defaultEncoding).encodeNoHeader(obj);
    }

    public Object decode(byte[] data) {
        Header info = getInfo(data);
        if (info == null) {
            IEncoder encoder =encodings.get(defaultEncoding);
            return decodeNoHeader(data, encoder);
        }
        if (!info.isEncoded()) {
            return removeHeader(data);
        } else {
            IEncoder encoder = encodings.get(info.getEncodingType().toString());;
            return encoder.decode(data);
        }
    }

    @Override
    public Object decodeNoHeader(byte[] data) {
        return decodeNoHeader(data, encodings.get(defaultEncoding));
    }


    @Override
    public Integer getEncodingType() {
        return 0;
    }
}
