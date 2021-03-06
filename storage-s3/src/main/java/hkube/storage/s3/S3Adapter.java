package hkube.storage.s3;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
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
import hkube.model.HeaderContentPair;
import hkube.storage.ISimplePathStorage;
import hkube.storage.IStorageConfig;

public class S3Adapter implements ISimplePathStorage {
    public static ISimplePathStorage getInstance() {
        return new S3Adapter();
    }

    AmazonS3 conn;

    S3Adapter() {

    }

    public void init(String accessKey, String secretKey,String endPoint) {
        AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
        conn = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, Regions.DEFAULT_REGION.toString())).build();
    }


    @Override
    public void put(String path, HeaderContentPair data) {
        ByteArrayInputStream input = new ByteArrayInputStream(data.getContent());
        ObjectMetadata metadata = new ObjectMetadata();
        byte [] header = data.getHeaderAsBytes();
        if(header != null) {
            metadata.addUserMetadata("header", Base64.getEncoder().encodeToString(data.getHeaderAsBytes()));
        }
        conn.putObject(getBucket(path), getPathInBucket(path), input, new ObjectMetadata());
    }

    @Override
    public HeaderContentPair get(String path) throws FileNotFoundException {
        byte[] tartgetArray;
        try {
            S3Object objectInfo = conn.getObject(getBucket(path), getPathInBucket(path));
            ObjectMetadata metadata = objectInfo.getObjectMetadata();
            String headerAsString = metadata.getUserMetaDataOf("header");
            byte[] headerBytes = null;
            if (headerAsString != null) {
                headerBytes = Base64.getDecoder().decode(headerAsString);
            }
            S3ObjectInputStream inputStream = objectInfo.getObjectContent();
            tartgetArray = new byte[inputStream.available()];
            inputStream.read(tartgetArray);
            HeaderContentPair pair = new HeaderContentPair(headerBytes,tartgetArray);
            return pair;
        } catch (AmazonS3Exception e) {
            if (e.getMessage().contains("NoSuchKey")) {
                throw new FileNotFoundException(path);
            } else throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    @Override
    public void setConfig(IStorageConfig config) {
        IS3Config s3Config = (IS3Config) config.getTypeSpecificConfig();
        String accessKey = s3Config.getAccessKeyId();
        String secretKey = s3Config.getSecretAccessKey();
        String endPoint = s3Config.getS3EndPoint();
        init(accessKey, secretKey,endPoint);
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
