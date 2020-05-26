package hkube.storage.s3;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.AmazonS3;
import hkube.storage.ISimplePathStorage;

public class S3Adapter implements ISimplePathStorage {
    public static ISimplePathStorage getInstance(){
        return new S3Adapter(new S3Config());
    }
    AmazonS3 conn;

    S3Adapter(S3Config configuration) {
        String accessKey = (String) configuration.getAccessKeyId();
        String secretKey = (String) configuration.getSecretAccessKey();
        init(accessKey, secretKey);
    }

    public void init(String accessKey, String secretKey) {
        AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
        conn = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://127.0.0.1:9000", Regions.DEFAULT_REGION.toString())).build();
    }


    @Override
    public void put(String path, byte[] data) {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        conn.putObject(getBucket(path), getPathInBucket(path), input, new ObjectMetadata());
    }

    @Override
    public byte[] get(String path) throws FileNotFoundException {
        byte[] tartgetArray;
        try {
            S3Object objectInfo = conn.getObject(getBucket(path), getPathInBucket(path));
            S3ObjectInputStream inputStream = objectInfo.getObjectContent();
            tartgetArray = new byte[inputStream.available()];
            inputStream.read(tartgetArray);
        } catch (AmazonS3Exception e) {
            if (e.getMessage().contains("NoSuchKey")) {
                throw new FileNotFoundException(path);
            } else throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tartgetArray;
    }

    @Override
    public List<String> list(String path) {
        ObjectListing objects = conn.listObjects(getBucket(path), getPathInBucket(path));
        List result = new ArrayList();
        do {
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                result.add(objectSummary.getKey());
            }
            objects = conn.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());
        return result;
    }

    @Override
    public void delete(String path) {
        conn.deleteObject(getBucket(path), getPathInBucket(path));
    }

    String getBucket(String completePath) {
        StringTokenizer tokenizer = new StringTokenizer(completePath, "/");
        return tokenizer.nextToken();
    }

    String getPathInBucket(String completePath) {
        String bucket = getBucket(completePath);
        if (completePath.startsWith("/")) {
            bucket = "/" + bucket;
        }
        bucket = bucket + "/";
        return completePath.replace(bucket, "");
    }
}
