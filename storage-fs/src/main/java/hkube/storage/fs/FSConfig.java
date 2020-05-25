package hkube.storage.fs;
import hkube.utils.Config;

public class FSConfig extends Config{
    public String getBaseDir() {
        return getStrEnvValue("BASE_FS_ADAPTER_DIRECTORY", "/var/tmp/fs/storage");
    }
}
