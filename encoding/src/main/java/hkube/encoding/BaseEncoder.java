package hkube.encoding;

import hkube.model.Header;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public abstract class BaseEncoder {

    public byte[] createHeader(boolean isEncoded) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write(Header.VERSION);
        stream.write(Header.FOOTER_LENGTH);
        if (isEncoded)
            stream.write(Header.DATA_TYPE_ENCODED);
        else
            stream.write(Header.DATA_TYPE_RAW);
        stream.write(getEncodingType());
        stream.write(Header.UNUSED);
        stream.write(Header.UNUSED);
        for (char c : Header.MAGIC_NUMBER) {
            stream.write(c);
        }
        return stream.toByteArray();
    }

    public static Header getInfo(byte[] data) {
        int headerEnd = ((int) data[1]);
        if (headerEnd > data.length || headerEnd<1) {
            return null;
        }
        byte[] headerBytes = Arrays.copyOfRange(data, 0, headerEnd);
        String magicNumber = new String(Arrays.copyOfRange(headerBytes,headerBytes.length-Header.MAGIC_NUMBER.length,headerBytes.length));
        if(!magicNumber.equals(new String(Header.MAGIC_NUMBER))){
            return null;
        }
        Header header = new Header(headerBytes);
        return header;
    }

    public ByteArrayInputStream getByteInputStreamNoHeader(byte[] data) {
        int headerLength = (int) data[1];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        byteArrayInputStream.read(new byte[headerLength],0, headerLength);
        return byteArrayInputStream;
    }

    public abstract Integer getEncodingType();

}
