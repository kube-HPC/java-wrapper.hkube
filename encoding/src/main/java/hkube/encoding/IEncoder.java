package hkube.encoding;


import hkube.model.Header;
import hkube.model.HeaderContentPair;

public interface IEncoder {



    HeaderContentPair encodeSeparately(Object obj);
    byte[] encodeNoHeader(Object obj);

    public Object decodeSeparately(Header header, byte[] data);
    Object decodeNoHeader(byte[] data);
    public Integer getEncodingType();
    public String getName();
}
