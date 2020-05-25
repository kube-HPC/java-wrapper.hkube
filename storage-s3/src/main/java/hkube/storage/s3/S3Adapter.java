package hkube.storage.s3;

import java.io.ByteArrayInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.StringUtils;
import com.amazonaws.services.s3.AmazonS3;

import hkube.storage.IAdapter;

public class S3Adapter implements IAdapter {
    //final static String  bucket = "local-hkube";
    final static String  bucket = "stambucket";
    AmazonS3 conn;

    S3Adapter(S3Config configuration) {
        String accessKey = (String) configuration.getAccessKeyId();
        String secretKey = (String) configuration.getSecretAccessKey();
        init(accessKey, secretKey);
    }

    public void init(String accessKey, String secretKey) {
        AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
        conn = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://127.0.0.1:9000", Regions.DEFAULT_REGION.toString())).build();

        List<Bucket> buckets = conn.listBuckets();
        for (Bucket bucket : buckets) {
            System.out.println(bucket.getName() + "\t" +
                    StringUtils.fromDate(bucket.getCreationDate()));
        }
    }


    @Override
    public void put(String path, byte[] data) {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        conn.putObject(bucket, path, input, new ObjectMetadata());
    }

    @Override
    public byte[] get(String path) throws FileNotFoundException {
        byte[] tartgetArray;
        try {
            S3Object objectInfo = conn.getObject(bucket, path);
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
        ObjectListing objects = conn.listObjects(bucket);
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
        conn.deleteObject(bucket, path);
    }
}
