package hkube.storage.test;

import hkube.model.HeaderContentPair;
import hkube.storage.ISimplePathStorage;
import hkube.storage.IStorageConfig;


import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TESTAdapter implements ISimplePathStorage {
    Map cach = new HashMap();
    public static TESTAdapter getInstance(){
        return new TESTAdapter();
    }
    @Override
    public void put(String path, HeaderContentPair data) {
        cach.put(path,data);
    }

    @Override
    public HeaderContentPair get(String path) throws FileNotFoundException {
        return (HeaderContentPair) cach.get(path);
    }

    @Override
    public List<String> list(String path) {
        return null;
    }

    @Override
    public void delete(String path) {

    }

    @Override
    public void setConfig(IStorageConfig config) {

    }
}
