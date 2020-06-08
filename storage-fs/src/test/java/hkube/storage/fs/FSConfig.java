package hkube.storage.fs;
import hkube.utils.Config;

public class FSConfig extends Config implements IFSConfig{
    public String getBaseDir() {
        return getStrEnvValue("BASE_FS_ADAPTER_DIRECTORY", "/var/tmp/fs/storage");
    }
}
