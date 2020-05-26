package hkube.storage;

import java.lang.reflect.Method;

public class StorageFactory {
    TaskStorage taskStorage;
    ISimplePathStorage storage;
    StorageConfig config;

    public StorageFactory( StorageConfig config) {
        this.config = config;
        String type = config.getStorageType();
        String className = "hkube.storage." + type + "." + type.toUpperCase() + "Adapter";
        try {
            Class claaz = this.getClass().getClassLoader().loadClass(className);
            Method method = claaz.getMethod("getInstance");
            storage = (ISimplePathStorage) method.invoke(null, null);
            taskStorage = new TaskStorage(storage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TaskStorage getTaskStorage() {
        return taskStorage;
    }

    public ISimplePathStorage getStorage() {
        return storage;
    }

}
