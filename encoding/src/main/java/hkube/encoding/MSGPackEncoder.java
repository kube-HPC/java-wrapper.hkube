package hkube.encoding;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.type.ValueType;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MSGPackEncoder implements IEncoder{

    @Override
    public byte[] encode(Map obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MessagePack msgpack = new MessagePack();
        Packer packer = msgpack.createPacker(out);
        try {
            packer.write(obj);
            packer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    @Override
    public Map decode(byte[] data) {
        MessagePack msgpack = new MessagePack();
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(data));
        try {
            MapValue mapValue = unpacker.readValue().asMapValue();
            Map result = new HashMap();
            for (Map.Entry<Value, Value> e : mapValue.entrySet()) {
                String key = e.getKey().asRawValue().getString();
                Object val = deserializeObject(e.getValue());
                result.put(key, val);
            }
            return result;
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }

    private Object deserializeObject(Value v) {
        ValueType t = v.getType();
        if (t == ValueType.NIL) {
            return null;
        } else if (t == ValueType.BOOLEAN) { // boolean
            return v.asBooleanValue().getBoolean();
        } else if (t == ValueType.INTEGER) { // integer, long, ...
            return v.asIntegerValue().getLong();
        } else if (t == ValueType.FLOAT) { // float, double, ..
            return v.asFloatValue().getFloat();
        } else if (t == ValueType.ARRAY) { // array
            ArrayList list = new ArrayList();
            ArrayValue arrayValue = v.asArrayValue();
            for(Value value:arrayValue){
                list.add(deserializeObject(value));
            }
            return list;
        } else if (t == ValueType.MAP) { // map
            MapValue mapValue = v.asMapValue();
            Map result = new HashMap();
            for (Map.Entry<Value, Value> e : mapValue.entrySet()) {
                String key = e.getKey().asRawValue().getString();
                Object val = deserializeObject(e.getValue());
                result.put(key, val);
            }
            return result;
        } else if (t == ValueType.RAW) { // string
            return v.asRawValue().getString();
        } else {
            throw new RuntimeException("fatal error");
        }
    }
}
