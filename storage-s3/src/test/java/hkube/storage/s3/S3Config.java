package hkube.storage.s3;

import hkube.utils.Config;

public class S3Config extends Config implements IS3Config {
    String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");

    public String getAccessKeyId() {
        return getStrEnvValue("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE");
    }
    public String getSecretAccessKey() {
        return getStrEnvValue("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    }

    @Override
    public String getS3EndPoint() {
        return "http://127.0.0.1:9000";
    }
}
