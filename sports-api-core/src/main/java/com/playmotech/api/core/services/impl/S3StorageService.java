package com.playmotech.api.core.services.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.playmotech.api.core.services.IStorageService;

import lombok.extern.log4j.Log4j2;

/**
 * Created By: deep.patel
 **/
@Service
@Log4j2
public class S3StorageService implements IStorageService {

    private final AmazonS3 s3Client;

    public S3StorageService(final AmazonS3 amazonS3) {
        this.s3Client = amazonS3;
    }

    @Override
    public void upload(String bucket, String key, byte[] fileContent) {
        upload(bucket, key, fileContent, null);
    }

    @Override
    public void upload(String bucket, String key, byte[] fileContent, String contentType) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(fileContent.length);
        if (contentType != null) {
            objectMetadata.setContentType(contentType);
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, new ByteArrayInputStream(fileContent),
                objectMetadata);
        putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
        s3Client.putObject(putObjectRequest);
    }

    @Override
    public void copy(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, sourceKey, destinationBucket,
                destinationKey);
        copyObjectRequest.withCannedAccessControlList(CannedAccessControlList.PublicRead);
        s3Client.copyObject(copyObjectRequest);
    }

    @Override
    public void delete(String bucket, String key) {
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucket, key);
        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public InputStream readInputStream(String bucket, String key) {
        System.out.println(bucket + ", " + key);
        S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucket, key));
        return s3Object.getObjectContent();
    }

    @Override
    public URL generatePreSignedUploadUrl(String bucket, String key, String contentType, int expiryInMilliSeconds) {
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + expiryInMilliSeconds * 1000L);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        // To make the object to be uploaded, publicly available
        generatePresignedUrlRequest.addRequestParameter("x-amz-acl", CannedAccessControlList.PublicRead.toString());

        if (contentType != null) {
            generatePresignedUrlRequest.addRequestParameter(Headers.CONTENT_TYPE, contentType);
        }

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url;
    }

    @Override
    public boolean doesObjectExist(String bucket, String key) {
        return s3Client.doesObjectExist(bucket, key);
    }

    @Override
    public byte[] download(String bucket, String key) {
        try {
            S3Object s3Object = s3Client.getObject(bucket, key);
            InputStream inputStream = s3Object.getObjectContent();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            inputStream.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error downloading object from s3: {}", e.getMessage());
            return null;
        }

    }

}