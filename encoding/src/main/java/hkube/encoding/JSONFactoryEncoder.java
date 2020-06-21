package hkube.encoding;
import com.fasterxml.jackson.databind.ObjectMapper;
import hkube.utils.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public abstract class JSONFactoryEncoder extends BaseEncoder implements IEncoder{
    final static int DATA_TYPE_ENCODED = 3;
    private static final Logger logger = LogManager.getLogger();
    private final JsonFactory factory;

    JSONFactoryEncoder(JsonFactory factory){
        this.factory = factory;
    }

    @Override
    public byte[] encode(Object obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(factory);
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
        timing.logInfo();
        return result;
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        Timing timing = new Timing(logger, "encode");
        timing.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(factory);
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

        byte[] encodedData = removeHeader(data);
        Timing timing = new Timing(logger, "decode");
        timing.start();
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            Object result = objectMapper.readValue(encodedData, Object.class);
            timing.end();
            timing.logInfo();
            return result;

        } catch (Throwable e) {
            return null;
        }

    }
    @Override
    public Object decodeNoHeader(byte[] data) {
        Timing timing = new Timing(logger, "decode");
        timing.start();
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            return objectMapper.readValue(data,Object.class);
        } catch (Throwable e) {
            return null;
        }
    }

}
