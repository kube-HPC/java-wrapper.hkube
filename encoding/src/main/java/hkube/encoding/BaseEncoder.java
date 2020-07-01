package hkube.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
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
        if (isEncoded)
            stream.write(DATA_TYPE_ENCODED);
        else
            stream.write(DATA_TYPE_RAW);
        stream.write(getEncodingType());
        stream.write(UNUSED);
        stream.write(UNUSED);
        for (char c : MAGIC_NUMBER) {
            stream.write(c);
        }
        return stream.toByteArray();
    }

    public Header getInfo(byte[] data) {
        int headerEnd = ((int) data[1]);
        if (headerEnd > data.length || headerEnd<1) {
            return null;
        }
        byte[] headerBytes = Arrays.copyOfRange(data, 0, headerEnd);
        String magicNumber = new String(Arrays.copyOfRange(headerBytes,headerBytes.length-MAGIC_NUMBER.length,headerBytes.length));
        if(!magicNumber.equals(new String(MAGIC_NUMBER))){
            return null;
        }
        Header header = new Header();
        header.setVersion((int) headerBytes[0]);
        header.setEncodingType((int) headerBytes[3]);
        header.setEncoded((int) headerBytes[2] == 2);
        return header;
    }

    public byte[] removeHeader(byte[] data) {
        int headerLength = (int) data[1];
        return Arrays.copyOfRange(data, headerLength, data.length);
    }
    public ByteArrayInputStream getByteInputStreamNoHeader(byte[] data) {
        int headerLength = (int) data[1];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        byteArrayInputStream.read(new byte[headerLength],0, headerLength);
        return byteArrayInputStream;
    }
    public ByteBuffer getByteBufferNoHeader(byte[] data) {
        int headerLength = (int) data[1];
        ByteBuffer buf = ByteBuffer.wrap(data,headerLength,data.length-headerLength);
        return buf;
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
