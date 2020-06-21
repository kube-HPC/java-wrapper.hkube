package hkube.encoding;
import com.fasterxml.jackson.core.JsonFactory;
public class JsonEncoder extends JSONFactoryEncoder {
    final static int DATA_TYPE_ENCODED = 2;
    public JsonEncoder(){
        super(new JsonFactory());
    }

    @Override
    public Integer getEncodingType() {
        return DATA_TYPE_ENCODED;
    }
    @Override
    public String getName() {
        return "json";
    }
}