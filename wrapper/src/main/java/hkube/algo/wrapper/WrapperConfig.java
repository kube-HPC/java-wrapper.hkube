package hkube.algo.wrapper;

import hkube.communication.ICommConfig;
import hkube.storage.IStorageConfig;
import hkube.storage.fs.IFSConfig;
import hkube.storage.s3.IS3Config;
import hkube.utils.Config;

public class WrapperConfig extends Config {
    public String getPort() {
        return getStrEnvValue("WORKER_SOCKET_PORT", "3000");
    }

    public String getHost() {
        return getStrEnvValue("WORKER_SOCKET_HOST", "localhost");
    }

    public String getAlgorithmClassName() {
        return getStrEnvValue("ALGORITHM_ENTRY_POINT", null);
    }

    public String getEncodingType() {
        return getStrEnvValue("WORKER_ALGORITHM_ENCODING", "bson");
    }

    public String getUrl() {
        try {
            return getStrEnvValue("WORKER_SOCKET_URL", null);
        } catch (Throwable e) {
            return null;
        }
    }


    public String getStorageVersion() {
        return "v2";
    }

    class CommConfig extends Config implements ICommConfig {
        public Integer getMaxCacheSize() {
            return getNumericEnvValue("DISCOVERY_MAX_CACHE_SIZE", 500);
        }

        public String getListeningPort() {
            return getStrEnvValue("DISCOVERY_PORT", "9020");
        }

        public Integer getStreamMaxBufferSize() {
            return getNumericEnvValue("STREAMING_MAX_BUFFER_MB", 1500);
        }

        public String getStreamListeningPort() {
            return getStrEnvValue("STREAMING_DISCOVERY_PORT", "9022");
        }

        public Integer getstreamstatisticsinterval() {
            return getNumericEnvValue("STREAMING_STATISTICS_INTERVAL", 2);
        }

        public Boolean isStateful() {
            return getStrEnvValue("STREAMING_STATEFUL", "True").equals("True");
        }

        public String getListeningHost() {
            return getStrEnvValue("POD_IP", "127.0.0.1");
        }

        public String getEncodingType() {
            return getStrEnvValue("DISCOVERY_ENCODING", "msgpack");
        }

        public Integer getTimeout() {

            return getNumericEnvValue("DISCOVERY_TIMEOUT", 20000);
        }

        public Integer getNetworkTimeout() {

            return getNumericEnvValue("DISCOVERY_NETWORK_TIMEOUT", 1000);
        }
    }

    public ICommConfig commConfig = new CommConfig();

    class StorageConfig extends Config implements IStorageConfig {
        public String getStorageType() {
            return getStrEnvValue("DEFAULT_STORAGE", "fs");
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

        public String getEncodingType() {

            return getStrEnvValue("STORAGE_ENCODING", "msgpack");
        }
    }

    public StorageConfig storageConfig = new StorageConfig();

    class FSConfig extends Config implements IFSConfig {
        public String getBaseDir() {
            return getStrEnvValue("BASE_FS_ADAPTER_DIRECTORY", "/var/tmp/fs/storage");
        }
    }

    public FSConfig fsConfig = new FSConfig();

    class S3Config extends Config implements IS3Config {
        public String getAccessKeyId() {
            return getStrEnvValue("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE");
        }

        public String getSecretAccessKey() {
            return getStrEnvValue("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        }

        @Override
        public String getS3EndPoint() {
            return getStrEnvValue("S3_ENDPOINT_URL", "http://127.0.0.1:9000");
        }

    }

    public S3Config s3Config = new S3Config();
}
