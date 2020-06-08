package hkube.storage;

import hkube.utils.Config;

public interface IStorageConfig {
    public String getStorageType();
    public String getClusterName();
    public Config getTypeSpecificConfig();
}
