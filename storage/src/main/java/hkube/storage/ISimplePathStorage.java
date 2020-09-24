package hkube.storage;

import hkube.model.HeaderContentPair;

import java.io.FileNotFoundException;
import java.util.List;

public interface ISimplePathStorage {
    public void put(String path, HeaderContentPair data);
    public HeaderContentPair get(String path) throws FileNotFoundException;
    public List<String> list(String path);
    public  void delete (String path);
    public void setConfig(IStorageConfig config);
}
