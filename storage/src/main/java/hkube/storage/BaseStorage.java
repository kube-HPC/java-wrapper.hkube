package hkube.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;

public abstract class BaseStorage {
    ISimplePathStorage adapter;
    IStorageConfig config;
    String rootName;

    IEncoder encoder;
    BaseStorage(ISimplePathStorage storage ,IStorageConfig config) {
        this.adapter = storage;
        this.config = config;
        encoder = new EncodingManager(config.getEncodingType());
        rootName = config.getClusterName() + "-" + getRootPrefix();
    }

    void put(String path, Object data) {
        byte[] encoded = encoder.encode(data);
        adapter.put(enhancePath(path), encoded);
    }

   Object get(String path) throws FileNotFoundException {
        byte[] encoded=  adapter.get(enhancePath(path));
        return encoder.decode(encoded);
    }
    public Object getByFullPath(String path) throws FileNotFoundException{
        byte[] encoded = adapter.get(path);
        return encoder.decode(encoded);
    }

    List<String> list(String path) {
        return adapter.list(enhancePath(path));
    }

    void delete(String path) {
        adapter.delete(enhancePath(path));
    }

    String enhancePath(String path) {
        return rootName + File.separator + path;
    }

    abstract String getRootPrefix();

}
