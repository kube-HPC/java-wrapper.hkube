package hkube.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;
import hkube.model.Header;
import hkube.model.HeaderContentPair;
import hkube.model.ObjectAndSize;

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

    ObjectAndSize get(String path) throws FileNotFoundException {
        HeaderContentPair encoded = adapter.get(enhancePath(path));
        Integer size = encoded.getContent().length;
        if (encoded.getHeader().isEncoded()) {
            Object value = encoder.decodeSeparately(encoded.getHeader(), encoded.getContent());
            return new ObjectAndSize(value, size);
        }
        Object value = encoded.getContent();
        return new ObjectAndSize(value, size);
    }

    public ObjectAndSize getByFullPath(String path) throws FileNotFoundException {
        HeaderContentPair encoded = adapter.get(path);
        Integer size = encoded.getContent().length;
        Object value;
        if (encoded.getHeader() == null || encoded.getHeader().isEncoded()) {
            value = encoder.decodeNoHeader(encoded.getContent());
        } else {
            value = encoded.getContent();
        }
        return new ObjectAndSize(value, size);
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
