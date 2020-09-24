package hkube.encoding;

import hkube.model.Header;
import hkube.model.HeaderContentPair;

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
    public HeaderContentPair encodeSeparately(Object obj) {
        return encodings.get(defaultEncoding).encodeSeparately(obj);
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        return encodings.get(defaultEncoding).encodeNoHeader(obj);
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
