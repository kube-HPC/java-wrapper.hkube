package hkube.model;

public class Header {
    public final static int VERSION = 1;
    public final static int FOOTER_LENGTH = 8;
    public final static int DATA_TYPE_RAW = 1;
    public final static int DATA_TYPE_ENCODED = 2;
    public final static int UNUSED = 0;
    public final static char[] MAGIC_NUMBER = {'H', 'K'};

    byte[] bytes = null;

    public byte[] getBytes() {
        return bytes;
    }

    public Header(byte[] headerBytes) {
        this.bytes = headerBytes;
        setVersion((int) headerBytes[0]);
        setEncodingType((int) headerBytes[3]);
        setEncoded((int) headerBytes[2] == 2);
    }

    public Integer getVersion() {
        return version;
    }

    void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isEncoded() {
        return isEncoded;
    }

    void setEncoded(boolean encoded) {
        isEncoded = encoded;
    }

    public Integer getEncodingType() {
        return encodingType;
    }

    void setEncodingType(Integer encodingType) {
        this.encodingType = encodingType;
    }

    Integer version;
    boolean isEncoded;
    Integer encodingType;

}
