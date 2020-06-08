package hkube.algo.wrapper;

import hkube.communication.ICommConfig;
import hkube.storage.IStorageConfig;
import hkube.storage.fs.IFSConfig;
import hkube.utils.Config;

public class WrapperConfig extends Config {
    String port;
    String host;
    String algorithmClassName;

    public String getPort() {
        return getStrEnvValue("WORKER_SOCKET_PORT", "3000");
    }

    public String getHost() {
        return getStrEnvValue("WORKER_SOCKET_HOST", "localhost");
    }

    public String getAlgorithmClassName() {
        return getStrEnvValue("ALGORITHM_ENTRY_POINT", null);
    }

    public String getStorageType() {
        return getStrEnvValue("STORAGE_TYPE", "fs");
    }

    public String getEncodingType() {
        return "msgpack";
    }

    public String getStorageEncodingType() {
        return "json";
    }

    public String getStorageVersion() {
        return "v2";
    }

    class CommConfig extends Config implements ICommConfig {
        public Integer getMaxCacheSize() {
            return getNumericEnvValue("DISCOVERY_MAX_CACHE_SIZE", 3);
        }

        public String getListeningPort() {
            return getStrEnvValue("DISCOVERY_PORT", "9020");
        }

        public Integer getTimeout() {
            return getNumericEnvValue("TIMEOUT", 20000);
        }
    }

    public ICommConfig commConfig = new CommConfig();

    class StorageConfig extends Config implements IStorageConfig {
        public String getStorageType() {
            return getStrEnvValue("STORAGE_TYPE", "fs");
        }

        public String getClusterName() {
            return getStrEnvValue("CLUSTER_NAME", "local");
        }

        public Config getTypeSpecificConfig() {
            if (getStorageType().equals("fs")) {
                return fsConfig;
            } else {
                return s3Config;
            }
        }
    }

    public StorageConfig storageConfig = new StorageConfig();

    class FSConfig extends Config implements IFSConfig {
        public String getBaseDir() {
            return getStrEnvValue("BASE_FS_ADAPTER_DIRECTORY", "/var/tmp/fs/storage");
        }
    }

    public FSConfig fsConfig = new FSConfig();

    class S3Config extends Config {
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");

        public String getAccessKeyId() {
            return getStrEnvValue("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE");
        }

        public String getSecretAccessKey() {
            return getStrEnvValue("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        }
    }

    public S3Config s3Config = new S3Config();
}
