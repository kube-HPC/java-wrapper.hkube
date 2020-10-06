package hkube.storage;

import hkube.model.Header;
import hkube.model.HeaderContentPair;
import hkube.model.ObjectAndSize;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;


public class TaskStorage extends BaseStorage {
    TaskStorage(ISimplePathStorage storage, IStorageConfig config) {
        super(storage, config);
    }


    public void put(String jobId, String taskId, HeaderContentPair data) {
        super.putEncoded(createPath(jobId, taskId), data);
    }
    public ObjectAndSize get(String jobId, String taskId) throws FileNotFoundException {
        return super.get(createPath(jobId, taskId));
    }

    public List<String> list(String jobId) {
        return super.list(jobId);
    }

    public void delete(String jobId, String taskId) {
        super.delete(createPath(jobId, taskId));
    }

    public static String createPath(String jobId, String taskId) {
        return jobId + File.separator + taskId;
    }

    public String createFullPath(String jobId, String taskId) {
        return enhancePath(createPath(jobId, taskId));
    }

    @Override
    String getRootPrefix() {
        return "hkube";
    }
}
