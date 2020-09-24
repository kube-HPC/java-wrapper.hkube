package hkube.model;

import hkube.model.Header;
import java.util.Arrays;

public class HeaderContentPair {
    final static char[] MAGIC_NUMBER = {'H', 'K'};
    byte[] header;
    byte[] content;

    public Header getHeader() {
        return getHeader(header);
    }
    public byte[] getHeaderAsBytes() {
        return header;
    }
    public byte[] getContent() {
        return content;
    }

    public HeaderContentPair(byte[] header, byte[] content) {
        this.header =header;
        this.content = content;
    }
    public static Header getHeader(byte[] data) {
        int headerEnd = ((int) data[1]);
        if (headerEnd > data.length || headerEnd<1) {
            return null;
        }
        byte[] headerBytes = Arrays.copyOfRange(data, 0, headerEnd);
        String magicNumber = new String(Arrays.copyOfRange(headerBytes,headerBytes.length-MAGIC_NUMBER.length,headerBytes.length));
        if(!magicNumber.equals(new String(MAGIC_NUMBER))){
            return null;
        }
        Header header = new Header(headerBytes);
        return header;
    }
}
