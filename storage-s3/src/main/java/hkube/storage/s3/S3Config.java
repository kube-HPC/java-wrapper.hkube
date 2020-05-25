package hkube.storage.s3;

import hkube.utils.Config;

public class S3Config extends Config {
    String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");

    public String getAccessKeyId() {
        return getStrEnvValue("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE");
    }
    public String getSecretAccessKey() {
        return getStrEnvValue("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    }
}
