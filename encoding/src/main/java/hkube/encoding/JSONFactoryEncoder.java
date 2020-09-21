package hkube.encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import hkube.utils.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;


public abstract class JSONFactoryEncoder extends BaseEncoder implements IEncoder {
    final static int DATA_TYPE_ENCODED = 3;
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final JsonFactory factory;

    JSONFactoryEncoder(JsonFactory factory) {
        this.factory = factory;
    }

    @Override
    public byte[] encode(Object obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoded encoded = encodeSeparately(obj);
        try {
            out.write(encoded.getHeader());
            out.write(encoded.getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] result = out.toByteArray();
        timing.end();
        timing.logInfo();
        return result;
    }

    public Encoded encodeSeparately(Object obj){
        Timing timing = new Timing(logger, "encode_separately");
        timing.start();
        byte[] header;
        byte[] encodedContent;
        bufferToBytes(obj);
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            header = createHeader(!(obj instanceof byte[]));
            if (obj instanceof byte[]) {
                encodedContent = (byte[]) obj;
            } else {
                encodedContent =  objectMapper.writeValueAsBytes(obj);
            }
            Encoded result = new Encoded(header,encodedContent);
            return  result;
        } catch (IOException e) {
            logger.error(e);
            return  null;
        }
        finally {
            timing.end();
            timing.logInfo();
        }
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(factory);
        bufferToBytes(obj);
        try {
            out.write(objectMapper.writeValueAsBytes(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] result = out.toByteArray();
        timing.end();
        timing.logInfo();
        return result;
    }

    @Override
    public Object decode(byte[] data) {
        Timing timing = new Timing(logger, "decode");
        ByteArrayInputStream encodedDataStream = getByteInputStreamNoHeader(data);
        timing.start();
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            Object result = objectMapper.readValue(encodedDataStream, Object.class);
            byteToByteBuffer(result);
            timing.end();
            timing.logInfo();
            return result;

        } catch (Throwable e) {
            return null;
        }

    }


    public Object decodeSeparately(Header header, byte[] data)  {
        return decodeNoHeader(data);
    }

    private void bufferToBytes(Object obj) {
        if (obj instanceof Map) {
            Set keys = ((Map) obj).keySet();
            keys.stream().forEach(key -> {
                Object value = ((Map) obj).get(key);
                if(value instanceof Map){
                    bufferToBytes(value);
                }
                if(value instanceof ByteBuffer){
                    if(((ByteBuffer) value).position()==0 && ((ByteBuffer) value).capacity()== ((ByteBuffer) value).array().length) {
                        ((Map) obj).put(key, ((ByteBuffer) value).array());
                    }
                    else{
                        byte[] byteArr = new byte[((ByteBuffer) value).remaining()];
                        ((ByteBuffer) value).get(byteArr);
                        ((Map) obj).put(key,byteArr);
                    }
                }
            });
        }
    }

    private void byteToByteBuffer(Object obj) {
        if (obj instanceof Map) {
            Set keys = ((Map) obj).keySet();
            keys.stream().forEach(key -> {
                Object value = ((Map) obj).get(key);
                if(value instanceof Map){
                    byteToByteBuffer(value);
                }
                if(value instanceof byte[]){
                    ((Map) obj).put(key,ByteBuffer.wrap((byte[])value));
                }
            });
        }
    }

    @Override
    public Object decodeNoHeader(byte[] data) {
        Timing timing = new Timing(logger, "decode");
        timing.start();
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            Object result = objectMapper.readValue(data, Object.class);
            timing.end();
            timing.logInfo();
            return result;
        } catch (Throwable e) {
            return null;
        }
    }

}
