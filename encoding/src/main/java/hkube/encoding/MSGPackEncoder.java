package hkube.encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import hkube.utils.Timing;

import org.msgpack.jackson.dataformat.MessagePackFactory;

public class MSGPackEncoder extends BaseEncoder implements IEncoder {
    final static int DATA_TYPE_ENCODED = 3;
    private static final Logger logger = LogManager.getLogger();

    @Override
    public byte[] encode(Object obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        try {
            out.write(createHeader(!(obj instanceof  byte[])));
            if(obj instanceof  byte[]) {
                out.write((byte[]) obj);
            }else {
                out.write(objectMapper.writeValueAsBytes(obj));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] result = out.toByteArray();
        timing.end();
        timing.logDebug();
        return result;
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        try {
            out.write(objectMapper.writeValueAsBytes(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] result = out.toByteArray();
        timing.end();
        timing.logDebug();
        return result;
    }

    @Override
    public Map decode(byte[] data) {

        byte[] encodedData = removeHeader(data);
        Timing timing = new Timing(logger, "decode");
        timing.start();
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        try {

            return objectMapper.readValue(encodedData, HashMap.class);
        } catch (Throwable e) {
            return null;
        }
    }
    @Override
    public Map decodeNoHeader(byte[] data) {
        Timing timing = new Timing(logger, "decode");
        timing.start();
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        try {
            return objectMapper.readValue(data, HashMap.class);
        } catch (Throwable e) {
            return null;
        }
    }
    @Override
    public Integer getEncodingType() {
        return DATA_TYPE_ENCODED;
    }

}
