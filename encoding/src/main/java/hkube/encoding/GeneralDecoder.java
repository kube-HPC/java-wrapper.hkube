package hkube.encoding;

public class GeneralDecoder extends BaseEncoder {

    public Object decodeNoHeader(byte[] data) {
        MSGPackEncoder encoder = new MSGPackEncoder();
        return encoder.decodeNoHeader(data);
    }

    public Object decode(byte[] data) {
        Header info = getInfo(data);
        if (!info.isEncoded()) {
            return removeHeader(data);
        } else {
            MSGPackEncoder encoder = new MSGPackEncoder();
            return encoder.decode(data);
        }
    }


    @Override
    public Integer getEncodingType() {
        return 0;
    }
}
