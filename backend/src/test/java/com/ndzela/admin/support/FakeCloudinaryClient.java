package com.ndzela.admin.support;

import com.ndzela.admin.config.properties.StorageProperties;
import com.ndzela.admin.infrastructure.storage.CloudinaryClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Cloudinary in memory: tests never reach the real account.
public class FakeCloudinaryClient extends CloudinaryClient {
    private final String cloudName;
    private final Map<String, byte[]> privateFiles = new ConcurrentHashMap<>();
    private final Map<String, byte[]> publicImages = new ConcurrentHashMap<>();

    public FakeCloudinaryClient(StorageProperties props) {
        super(null, props, null);
        this.cloudName = props.cloudName();
    }

    @Override
    public String uploadPublicImage(byte[] bytes, String publicId) {
        publicImages.put(publicId, bytes);
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/v1/" + publicId + ".png";
    }

    @Override
    public void uploadPrivateFile(byte[] bytes, String publicId) {
        privateFiles.put(publicId, bytes);
    }

    @Override
    public byte[] downloadPrivateFile(String publicId) {
        return privateFiles.get(publicId);
    }

    @Override
    public void deletePrivateFile(String publicId) {
        privateFiles.remove(publicId);
    }

    @Override
    public void deletePublicImage(String publicId) {
        publicImages.remove(publicId);
    }

    public int privateCount() {
        return privateFiles.size();
    }

    public Map<String, byte[]> privateFiles() {
        return privateFiles;
    }

    public int publicCount() {
        return publicImages.size();
    }

    public void clear() {
        privateFiles.clear();
        publicImages.clear();
    }
}
