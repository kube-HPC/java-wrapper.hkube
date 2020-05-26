package hkube.storage.test;

import hkube.storage.ISimplePathStorage;

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
    public void put(String path, byte[] data) {
        cach.put(path,data);
    }

    @Override
    public byte[] get(String path) throws FileNotFoundException {
        return (byte[]) cach.get(path);
    }

    @Override
    public List<String> list(String path) {
        return null;
    }

    @Override
    public void delete(String path) {

    }
}
