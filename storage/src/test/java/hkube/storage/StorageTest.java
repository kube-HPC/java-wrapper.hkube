package hkube.storage;


import hkube.encoding.EncodingManager;
import hkube.model.HeaderContentPair;
import org.junit.Test;

import java.io.FileNotFoundException;


public class StorageTest {

    IStorageConfig config = new StorageConfig();
    @Test
    public void testStorageFactory() throws FileNotFoundException {
        StorageFactory factory = new StorageFactory(config);
        TaskStorage taskStorage = factory.getTaskStorage();
        ISimplePathStorage sotrage = factory.getStorage();
        EncodingManager manager = new EncodingManager("msgpack");
        byte[] headerBytes = manager.createHeader(false);
        taskStorage.put("job1","task1",new HeaderContentPair(headerBytes,"job1_task1".getBytes()));
        taskStorage.put("job2","task2", new HeaderContentPair(headerBytes,"job2_task2".getBytes()));
        sotrage.put("/bucket1/path1",new HeaderContentPair(null,"path1".getBytes()));
        sotrage.put("/bucket1/path2",new HeaderContentPair(null,"path2".getBytes()));
        byte[] arr  = (byte[]) taskStorage.get("job1","task1").getValue();
        String job1_task1 =  new String(arr);
        assert job1_task1.equals("job1_task1");
        String path1 = new String( sotrage.get("/bucket1/path1").getContent());
        assert path1.equals("path1");

    }
}