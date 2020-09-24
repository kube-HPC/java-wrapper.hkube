package hkube.storage.fs;

import hkube.encoding.EncodingManager;
import hkube.model.HeaderContentPair;
import hkube.storage.ISimplePathStorage;
import hkube.storage.IStorageConfig;
import hkube.utils.Config;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.Assert.assertThrows;

public class TestFS {
    FSConfig config = new FSConfig();

    @After
    public void deleteFiles() {
        String baseDir = config.getBaseDir();
        File dir = new File(baseDir);
        dir.delete();
    }

    @Test
    public void testPutGet() throws FileNotFoundException {
        ISimplePathStorage adapter = new FSAdapter();
        adapter.setConfig(storageConfig);
        EncodingManager encodingManager = new EncodingManager("msgpack");
        byte[] header = encodingManager.createHeader(false);
        adapter.put("dir1" + File.separator + "dir2" + File.separator + "Stam", new HeaderContentPair(header, "Kloom".getBytes()));
        String output = new String(adapter.get("dir1" + File.separator + "dir2" + File.separator + "Stam").getContent());
        assert output.equals("Kloom");
    }

    @Test
    public void testList() throws FileNotFoundException {
        ISimplePathStorage adapter = new FSAdapter();
        adapter.setConfig(storageConfig);
        adapter.put("dir1" + File.separator + "dir2" + File.separator + "Stam", new HeaderContentPair(null, "Kloom".getBytes()));
        adapter.put("dir1" + File.separator + "dir3" + File.separator + "Stam", new HeaderContentPair(null, "Kloom".getBytes()));
        List dirContent = adapter.list("dir1");
        assert dirContent.size() == 2;
        assert dirContent.contains(File.separator + "dir1" + File.separator + "dir3" + File.separator + "Stam");
        assert dirContent.contains(File.separator + "dir1" + File.separator + "dir2" + File.separator + "Stam");
    }

    @Test
    public void notFoundException() {
        assertThrows(FileNotFoundException.class, () -> {

            ISimplePathStorage adapter = new FSAdapter();
            adapter.setConfig(storageConfig);
            adapter.get("dir1" + File.separator + "dir2" + File.separator + "Ain");
        });
    }
    IStorageConfig storageConfig = new IStorageConfig() {
        @Override
        public String getStorageType() {
            return null;
        }

        @Override
        public String getClusterName() {
            return null;
        }

        @Override
        public Config getTypeSpecificConfig() {
            return config;
        }

        @Override
        public String getEncodingType() {
            return "msgpack";
        }
    };
}
