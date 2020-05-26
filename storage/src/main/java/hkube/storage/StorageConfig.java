package hkube.storage;

import hkube.utils.Config;

public class StorageConfig extends Config {
    public String getStorageType() {
        return getStrEnvValue("STORAGE_TYPE", "fs");
    }

    public String getClusterName() {
        return getStrEnvValue("CLUSTER_NAME", "local");
    }
}
