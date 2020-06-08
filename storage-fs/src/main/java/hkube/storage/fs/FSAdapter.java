package hkube.storage.fs;

import hkube.storage.ISimplePathStorage;
import hkube.storage.IStorageConfig;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FSAdapter implements ISimplePathStorage {
    public static ISimplePathStorage getInstance(){
        return new FSAdapter();
    }
    File basePath;
    public FSAdapter() {

    }

    public void put(String path, byte[] data) {
        File file = new File(basePath.getAbsoluteFile() + File.separator + path);
        File parentDir = new File(file.getParent());
        if (!parentDir.isDirectory()) {
            try {
                Files.createDirectories(Paths.get(parentDir.getAbsolutePath()));
            } catch (IOException e) {
                throw new RuntimeException("Unable to create directory " + parentDir.getAbsolutePath());
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] get(String path) throws FileNotFoundException {
        File file = new File(basePath.getAbsoluteFile() + File.separator + path);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath() + " does not exist");
        }
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] data = fis.readAllBytes();
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read" + file.getAbsolutePath());
        }
    }

    public void delete(String path) {
        File file = new File(basePath.getAbsoluteFile() + File.separator + path);
        file.delete();
    }

    @Override
    public void setConfig(IStorageConfig config) {
        IFSConfig fsConfig = (IFSConfig) config.getTypeSpecificConfig();
        String basePathStr = fsConfig.getBaseDir();
        if (basePathStr != null) {
            basePath = new File(basePathStr);
            if (!basePath.isDirectory()) {
                basePath.mkdir();
            }
        } else {
            throw new RuntimeException("missing baseDirectory configuration parameter");
        }
    }

    public List<String> list(String path) {
        return listPrefix(path);
    }

    public List<String> listPrefix(String path) {
        try (Stream<Path> walk = Files.walk(Paths.get(basePath.getAbsoluteFile() + File.separator + path))) {
            List<String> fullPathResult = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
            return fullPathResult.stream().map(fullPath -> fullPath.replace(basePath.getAbsolutePath(), "")).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

