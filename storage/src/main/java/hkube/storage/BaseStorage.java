package hkube.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import hkube.encoding.GeneralDecoder;
import hkube.encoding.MSGPackEncoder;

public abstract class BaseStorage {
    ISimplePathStorage adapter;
    StorageConfig config = new StorageConfig();
    String rootName = config.getClusterName() + "-" + getRootPrefix();
    MSGPackEncoder encoder = new MSGPackEncoder();
    GeneralDecoder decoder = new GeneralDecoder();
    BaseStorage(ISimplePathStorage storage) {
        this.adapter = storage;
    }

    void put(String path, Object data) {
        byte[] encoded = encoder.encode(data);
        adapter.put(enhancePath(path), encoded);
    }

   Object get(String path) throws FileNotFoundException {
        byte[] encoded=  adapter.get(enhancePath(path));
        return decoder.decode(encoded);
    }
    public Object getByFullPath(String path) throws FileNotFoundException{
        byte[] encoded = adapter.get(path);
        return decoder.decode(encoded);
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
