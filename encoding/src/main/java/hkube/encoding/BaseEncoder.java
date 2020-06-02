package hkube.encoding;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public abstract class BaseEncoder {
    final static int VERSION = 1;
    final static int FOOTER_LENGTH = 8;
    final static int DATA_TYPE_RAW = 1;
    final static int DATA_TYPE_ENCODED = 2;
    final static int UNUSED = 0;
    final static char[] MAGIC_NUMBER = {'H', 'K'};

    public byte[] createHeader(boolean isEncoded) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write(VERSION);
        stream.write(FOOTER_LENGTH);
        stream.write(getEncodingType());
        if (isEncoded)
            stream.write(DATA_TYPE_ENCODED);
        else
            stream.write(DATA_TYPE_RAW);
        stream.write(UNUSED);
        stream.write(UNUSED);
        for (char c : MAGIC_NUMBER) {
            stream.write(c);
        }
        return stream.toByteArray();
    }

    public Header getInfo(byte[] data) {
        int headeerEnd = ((int) data[1]);
        byte[] headerBytes = Arrays.copyOfRange(data, 0, headeerEnd);
        Header header = new Header();
        header.setVersion((int) headerBytes[0]);
        header.setEncodingType((int) headerBytes[2]);
        header.setEncoded((int) headerBytes[3] == 2);
        return header;
    }

    public byte[] removeHeader(byte[] data) {
        int headerLength = (int) data[1];
        return Arrays.copyOfRange(data, headerLength, data.length);
    }

    public abstract Integer getEncodingType();

    public static class Header {
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
}
