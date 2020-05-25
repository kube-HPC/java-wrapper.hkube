package hkube.storage;

import java.io.FileNotFoundException;
import java.util.List;

public interface IAdapter {
    public void put(String path, byte[] data);
    public byte[] get(String path) throws FileNotFoundException;
    public List<String> list(String path);
    public  void delete (String path);
}
