package hkube.encoding;


public interface IEncoder {


    byte[] encode(Object obj);
    Encoded encodeSeparately(Object obj);
    byte[] encodeNoHeader(Object obj);

    public Object decode(byte[] data);
    public Object decodeSeparately(Header header, byte[] data);
    Object decodeNoHeader(byte[] data);
    public Integer getEncodingType();
    public String getName();
}
