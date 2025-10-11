package com.playmotech.api.core.services;

import java.io.InputStream;
import java.net.URL;

/**
 * Created By: deep.patel
 **/
public interface IStorageService {
    void upload(String bucket, String key, byte[] fileContent);

    void upload(String bucket, String key, byte[] fileContent, String contentType);

    void copy(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey);

    void delete(String bucket, String key);

    InputStream readInputStream(String bucket, String key);

    /**
     * Method which generates pre-signed upload URL.
     * 
     * @param bucket          Bucket name
     * @param key             Object key
     * @param contentType     Object's content type
     * @param expiryInSeconds Expiry in seconds
     * @return URL Object
     */
    URL generatePreSignedUploadUrl(String bucket, String key, String contentType, int expiryInSeconds);

    boolean doesObjectExist(String bucket, String key);

    public byte[] download(String bucket, String key);
}
