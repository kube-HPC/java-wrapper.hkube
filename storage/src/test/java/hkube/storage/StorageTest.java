package hkube.storage;


import org.junit.Test;

import java.io.FileNotFoundException;

public class StorageTest {

    IStorageConfig config = new StorageConfig() {
        @Override
        public String getStorageType() {
            return "test";
        }

    };
    @Test
    public void testStorageFactory() throws FileNotFoundException {
        StorageFactory factory = new StorageFactory(config);
        TaskStorage taskStorage = factory.getTaskStorage();
        ISimplePathStorage sotrage = factory.getStorage();
        taskStorage.put("job1","task1","job1_task1".getBytes());
        taskStorage.put("job2","task2","job2_task2".getBytes());
        sotrage.put("/bucket1/path1","path1".getBytes());
        sotrage.put("/bucket1/path2","path2".getBytes());
        String job1_task1 =  new String((byte[])taskStorage.get("job1","task1"));
        assert job1_task1.equals("job1_task1");
        String path1 = new String( sotrage.get("/bucket1/path1"));
        assert path1.equals("path1");

    }
}