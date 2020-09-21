package hkube.encoding;

public class Header {
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isEncoded() {
        return isEncoded;
    }

    public void setEncoded(boolean encoded) {
        isEncoded = encoded;
    }

    public Integer getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(Integer encodingType) {
        this.encodingType = encodingType;
    }

    Integer version;
    boolean isEncoded;
    Integer encodingType;

}
