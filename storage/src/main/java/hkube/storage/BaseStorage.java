package hkube.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;
import hkube.model.Header;
import hkube.model.HeaderContentPair;

public abstract class BaseStorage {
    ISimplePathStorage adapter;
    IStorageConfig config;
    String rootName;

    IEncoder encoder;

    BaseStorage(ISimplePathStorage storage, IStorageConfig config) {
        this.adapter = storage;
        this.config = config;
        encoder = new EncodingManager(config.getEncodingType());
        rootName = config.getClusterName() + "-" + getRootPrefix();
    }

    void putEncoded(String path, HeaderContentPair data) {
        adapter.put(enhancePath(path), data);
    }

    Object get(String path) throws FileNotFoundException {
        HeaderContentPair encoded = adapter.get(enhancePath(path));
        if(encoded.getHeader().isEncoded()){
            if(encoded.getHeader() != null){
                return encoder.decodeSeparately(encoded.getHeader(),encoded.getContent());
            }
            return  encoder.decodeNoHeader(encoded.getContent());
        }
        return encoded.getContent();
    }

    public Object getByFullPath(String path) throws FileNotFoundException {
        HeaderContentPair encoded = adapter.get(path);
        return encoder.decodeNoHeader(encoded.getContent());
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
