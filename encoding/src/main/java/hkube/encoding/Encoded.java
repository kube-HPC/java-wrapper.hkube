package hkube.encoding;

public class Encoded {
    byte[] header;

    public byte[] getHeader() {
        return header;
    }

    public byte[] getContent() {
        return content;
    }

    public Encoded(byte[] header, byte[] content) {
        this.header = header;
        this.content = content;
    }

    byte[] content;
}
