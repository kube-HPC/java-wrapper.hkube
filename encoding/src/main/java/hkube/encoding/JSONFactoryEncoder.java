package hkube.encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import hkube.model.Header;
import hkube.model.HeaderContentPair;
import hkube.utils.Timing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;



public abstract class JSONFactoryEncoder extends BaseEncoder implements IEncoder {
    final static int DATA_TYPE_ENCODED = 3;
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final JsonFactory factory;

    JSONFactoryEncoder(JsonFactory factory) {
        this.factory = factory;
    }



    public HeaderContentPair encodeSeparately(Object obj){
        byte[] header;
        byte[] encodedContent;
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            header = createHeader(!(obj instanceof byte[]));
            if (obj instanceof byte[]) {
                encodedContent = (byte[]) obj;
            } else {
                encodedContent =  objectMapper.writeValueAsBytes(obj);
            }
            HeaderContentPair result = new HeaderContentPair(header,encodedContent);
            return  result;
        } catch (IOException e) {
            logger.error(e);
            return  null;
        }
    }

    @Override
    public byte[] encodeNoHeader(Object obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            out.write(objectMapper.writeValueAsBytes(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] result = out.toByteArray();
        return result;
    }




    public Object decodeSeparately(Header header, byte[] data)  {
        return decodeNoHeader(data);
    }





    @Override
    public Object decodeNoHeader(byte[] data) {
        ObjectMapper objectMapper = new ObjectMapper(factory);
        try {
            Object result = objectMapper.readValue(data, Object.class);
            return result;
        } catch (Throwable e) {
            return null;
        }
    }

}
