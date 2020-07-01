package hkube.storage;


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class StorageTest {

    IStorageConfig config = new StorageConfig();
    @Test
    public void testStorageFactory() throws FileNotFoundException {
        StorageFactory factory = new StorageFactory(config);
        TaskStorage taskStorage = factory.getTaskStorage();
        ISimplePathStorage sotrage = factory.getStorage();
        taskStorage.put("job1","task1","job1_task1".getBytes());
        taskStorage.put("job2","task2","job2_task2".getBytes());
        sotrage.put("/bucket1/path1","path1".getBytes());
        sotrage.put("/bucket1/path2","path2".getBytes());
        ByteBuffer buf = (ByteBuffer)taskStorage.get("job1","task1");
        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        String job1_task1 =  new String(arr);
        assert job1_task1.equals("job1_task1");
        String path1 = new String( sotrage.get("/bucket1/path1"));
        assert path1.equals("path1");

    }
}