package hkube.storage.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import hkube.storage.IAdapter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TestS3 {
    S3Config config = new S3Config();


    @After
    public void deleteFiles() {
        AWSCredentials creds = new BasicAWSCredentials(config.getAccessKeyId(), config.getSecretAccessKey());
        AmazonS3 conn = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://127.0.0.1:9000", Regions.DEFAULT_REGION.toString())).build();

        ObjectListing objects = conn.listObjects(S3Adapter.bucket);
        List result = new ArrayList();
        do {
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                conn.deleteObject(S3Adapter.bucket,objectSummary.getKey());
            }
            objects = conn.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());
    }

    @Test
    public void testPutGet() throws FileNotFoundException {
        IAdapter adapter = new S3Adapter(config);
        adapter.put("dir1" + File.separator + "dir2" + File.separator + "Stam", "Kloom".getBytes());
        String output = new String(adapter.get("dir1" + File.separator + "dir2" + File.separator + "Stam"));
        assert output.equals("Kloom");
    }
    @Test
    public void testList() throws FileNotFoundException {
        IAdapter adapter = new S3Adapter(config);
        adapter.put("dir1" + File.separator + "dir2" + File.separator + "Stam", "Kloom".getBytes());
        adapter.put("dir1" + File.separator + "dir3" + File.separator + "Stam", "Kloom".getBytes());
        List dirContent = adapter.list("dir1");
        assert dirContent.size()==2;
        assert dirContent.contains("dir1" + File.separator + "dir3" + File.separator + "Stam");
        assert dirContent.contains("dir1" + File.separator + "dir2" + File.separator + "Stam");
    }

    @Test
    public void notFoundException() {
        Assert.assertThrows(FileNotFoundException.class, () -> {
            IAdapter adapter = new S3Adapter(config);
            adapter.get("dir1" + File.separator + "dir2" + File.separator + "Ain");
        });
    }
}
