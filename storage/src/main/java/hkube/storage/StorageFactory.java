package hkube.storage;

import hkube.utils.Config;

import java.lang.reflect.Method;

public class StorageFactory {
    TaskStorage taskStorage;
    ISimplePathStorage storage;
    IStorageConfig config;

    public StorageFactory( IStorageConfig config) {
        this.config = config;
        String type = config.getStorageType();
        String className = "hkube.storage." + type + "." + type.toUpperCase() + "Adapter";
        try {
            Class claaz = this.getClass().getClassLoader().loadClass(className);
            Method method = claaz.getMethod("getInstance");
            storage = (ISimplePathStorage) method.invoke(null, null);
            storage.setConfig(config);
            taskStorage = new TaskStorage(storage,config);
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

    public IStorageConfig getConfig(){
        return  this.config;
    }

}
