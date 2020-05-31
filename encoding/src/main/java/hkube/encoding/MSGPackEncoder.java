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
    public byte[] encode(Map obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        try {
            out.write(objectMapper.writeValueAsBytes(obj));
            out.write(createFooter());
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
        Footer info = getInfo(data);
        byte[] encodedData = removeFooter(data);
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
    public Integer getEncodingType() {
        return DATA_TYPE_ENCODED;
    }
}
