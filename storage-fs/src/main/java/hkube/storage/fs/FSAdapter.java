package hkube.storage.fs;

import hkube.model.Header;
import hkube.model.HeaderContentPair;
import hkube.storage.ISimplePathStorage;
import hkube.storage.IStorageConfig;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FSAdapter implements ISimplePathStorage {
    public static ISimplePathStorage getInstance() {
        return new FSAdapter();
    }

    File basePath;

    public FSAdapter() {

    }

    public void put(String path, HeaderContentPair data) {
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
            byte[] header = data.getHeaderAsBytes();
            if (header != null) {
                fos.write(header);
            }
            fos.write(data.getContent());
            fos.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HeaderContentPair get(String path) throws FileNotFoundException {

        File file = new File(basePath.getAbsoluteFile() + File.separator + path);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath() + " does not exist");
        }
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] versionAndSize = new byte[2];

            fis.read(versionAndSize);
            int headerSize = versionAndSize[1];
            if (headerSize > 3) {
                byte[] restOfHeader = new byte[headerSize - 2];
                int readBytes = fis.read(restOfHeader, 0, headerSize - 2);
                if (readBytes >= headerSize - 2) {
                    String magicNumber = new String(Arrays.copyOfRange(restOfHeader, restOfHeader.length - Header.MAGIC_NUMBER.length, restOfHeader.length));
                    if (magicNumber.equals(new String(Header.MAGIC_NUMBER))) {
                        byte[] data = fis.readAllBytes();
                        byte[] header = new byte[headerSize];
                        System.arraycopy(versionAndSize, 0, header, 0, 2);
                        System.arraycopy(restOfHeader, 0, header, 2, headerSize - 2);
                        return new HeaderContentPair(header, data);
                    }
                }
            }
            fis.close();
            fis = new FileInputStream(file);
            byte[] data = fis.readAllBytes();
            return new HeaderContentPair(null, data);

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

