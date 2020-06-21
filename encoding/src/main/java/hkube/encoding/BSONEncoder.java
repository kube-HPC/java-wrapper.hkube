package hkube.encoding;
import de.undercouch.bson4jackson.BsonFactory;
public class BSONEncoder extends JSONFactoryEncoder {
    final static int DATA_TYPE_ENCODED = 1;
    public BSONEncoder(){
        super(new BsonFactory());
    }

    @Override
    public Integer getEncodingType() {
        return DATA_TYPE_ENCODED;
    }
    @Override
    public String getName() {
        return "bson";
    }
}
