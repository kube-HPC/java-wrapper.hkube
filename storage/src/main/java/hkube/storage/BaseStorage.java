package hkube.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public abstract class BaseStorage {
    ISimplePathStorage adapter;
    StorageConfig config = new StorageConfig();
    String rootName = config.getClusterName() + "-" + getRootPrefix();

    BaseStorage(ISimplePathStorage storage) {
        this.adapter = storage;
    }

    void put(String path, byte[] data) {
        adapter.put(enhancePath(path), data);
    }

    byte[] get(String path) throws FileNotFoundException {
        return adapter.get(enhancePath(path));
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
