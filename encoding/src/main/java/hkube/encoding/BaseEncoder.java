package hkube.encoding;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public abstract class BaseEncoder {
    final static int VERSION = 1;
    final static int FOOTER_LENGTH = 8;

    final static int DATA_TYPE_ENCODED = 2;
    final static int UNUSED = 0;
    final static char[] MAGIC_NUMBER = {'H', 'K'};

    public byte[] createFooter() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write(VERSION);
        stream.write(getEncodingType());
        stream.write(DATA_TYPE_ENCODED);
        stream.write(UNUSED);
        stream.write(UNUSED);
        stream.write(FOOTER_LENGTH);
        for (char c : MAGIC_NUMBER) {
            stream.write(c);
        }
        return stream.toByteArray();
    }
    public Footer getInfo(byte[] data){
        int footerStart = data.length - ((int) data[data.length - MAGIC_NUMBER.length - 1]);
        byte [] footerBytes  = Arrays.copyOfRange(data,footerStart,data.length);
        Footer footer = new Footer();
        footer.setVersion((int)footerBytes[0]);
        footer.setEncoded((int)footerBytes[2]==2);
        footer.setEncodingType((int)footerBytes[1]);
        return footer;
    }
    public byte[] removeFooter(byte[] data){
        int footerLength = (int)data[data.length-MAGIC_NUMBER.length-1];
        return Arrays.copyOfRange(data,0,data.length -footerLength);
    }
    public abstract Integer getEncodingType();

    public static class Footer {
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
