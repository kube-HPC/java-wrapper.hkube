package hkube.encoding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class MSGPackEncoder extends JSONFactoryEncoder {
    final static int DATA_TYPE_ENCODED = 3;
    private static final Logger logger = LogManager.getLogger();
    public MSGPackEncoder(){
        super(new MessagePackFactory());
    }
    @Override
    public Integer getEncodingType() {
        return DATA_TYPE_ENCODED;
    }
    @Override
    public String getName() {
        return "msgpack";
    }
}
