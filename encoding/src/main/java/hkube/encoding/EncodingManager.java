package hkube.encoding;

import java.util.HashMap;
import java.util.Map;

public class EncodingManager extends BaseEncoder implements IEncoder {
    Map<String, IEncoder> encodings = new HashMap();
    String defaultEncoding;

    public EncodingManager(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
        addEncoder(new JsonEncoder());
        addEncoder(new MSGPackEncoder());
        addEncoder(new BSONEncoder());
    }

    void addEncoder(IEncoder encoder) {
        encodings.put(encoder.getEncodingType() + "", encoder);
        encodings.put(encoder.getName(), encoder);
    }

    private Object decodeNoHeader(byte[] data, IEncoder encoder) {

        return encoder.decodeNoHeader(data);
    }

    @Override
    public byte[] encode(Object obj) {
        return encodings.get(defaultEncoding).encode(obj);
    }

    @Override
    public Encoded encodeSeparately(Object obj) {
        return encodings.get(defaultEncoding).encodeSeparately(obj);
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        return encodings.get(defaultEncoding).encodeNoHeader(obj);
    }

    public Object decode(byte[] data) {
        Header info = getInfo(data);
        if (info == null) {
            IEncoder encoder = encodings.get(defaultEncoding);
            return decodeNoHeader(data, encoder);
        }
        if (!info.isEncoded()) {
            return getByteBufferNoHeader(data);
        } else {
            IEncoder encoder = encodings.get(info.getEncodingType().toString());
            return encoder.decode(data);
        }
    }

    @Override
    public Object decodeSeparately(Header header, byte[] data) {
        if (!header.isEncoded()) {
            return data;
        } else {
            IEncoder encoder = encodings.get(header.getEncodingType().toString());
            return encoder.decodeNoHeader(data);
        }
    }

    @Override
    public Object decodeNoHeader(byte[] data) {
        return decodeNoHeader(data, encodings.get(defaultEncoding));
    }


    @Override
    public Integer getEncodingType() {
        return encodings.get(defaultEncoding).getEncodingType();
    }

    @Override
    public String getName() {
        return encodings.get(defaultEncoding).getName();
    }
}
