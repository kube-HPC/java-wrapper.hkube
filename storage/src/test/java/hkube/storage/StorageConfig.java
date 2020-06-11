package hkube.storage;

import hkube.utils.Config;

public class StorageConfig extends Config implements IStorageConfig {
    public String getStorageType() {
        return getStrEnvValue("STORAGE_TYPE", "fs");
    }

    public String getClusterName() {
        return getStrEnvValue("CLUSTER_NAME", "local");
    }
    public Config getTypeSpecificConfig(){return  null;}

    @Override
    public String getEncodingType() {
        return "msgpack";
    }

}
