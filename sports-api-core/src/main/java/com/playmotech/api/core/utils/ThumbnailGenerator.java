package com.playmotech.api.core.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.exceptions.ResourceException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThumbnailGenerator {

    /**
     * Generate thumbnail from video bytes directly
     * 
     * @param videoBytes The raw bytes of the video file
     * @param fileName   Original filename of the video
     * @return FileObjectDto containing the generated thumbnail or null if
     *         generation fails
     * @throws ResourceException if an error occurs during thumbnail generation
     */
    public static FileObjectDto generateThumbnailFromBytes(byte[] videoBytes, String fileName)
            throws ResourceException {
        log.info("Generating thumbnail from bytes for: {}", fileName);

        // Detecting content type using Apache Tika
        Tika tika = new Tika();
        String contentType = tika.detect(videoBytes);
        log.info("Detected content type: {}", contentType);

        if (contentType != null && contentType.startsWith("video/")) {
            log.info("Processing video file for thumbnail generation");
            return generateVideoThumbnailBase64(videoBytes, fileName);
        } else {
            log.warn("Content is not a video file, cannot generate thumbnail");
            return null;
        }
    }

    public static FileObjectDto generateThumbnail(MultipartFile file) throws ResourceException {
        log.info("A file received for thumbnail generation: {}", file.getOriginalFilename());
        log.info("Generating thumbnail...");
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();

            // Detecting content type using Apache Tika
            Tika tika = new Tika();
            String contentType = tika.detect(fileBytes);
            log.info("contentType: {}", contentType);
            if (contentType != null) {
                // if (contentType.startsWith("image/")) {
                // log.info("contentType :{} - fileName: {}", contentType,
                // file.getOriginalFilename());
                // return generateImageThumbnailBase64(fileBytes);
                // } else
                if (contentType.startsWith("video/")) {
                    log.info("contentType: {} - fileName: {}", contentType, file.getOriginalFilename());
                    return generateVideoThumbnailBase64Async(fileBytes, file.getOriginalFilename());
                }
            }
            log.info("contentType is null :( ");
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Something unexpected happened during thumbnail generation: {}", e.getMessage());
            return null;
        }
        return null;
    }

    private static FileObjectDto generateImageThumbnailBase64(byte[] imageBytes) throws ResourceException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage;
        try {
            log.info("Generating thumbnail for image");
            originalImage = ImageIO.read(inputStream);

            int width = 200;
            int height = (originalImage.getHeight() * width) / originalImage.getWidth();

            BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();

            g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", outputStream);

            return buildFileObject("imageThumbnail.jpg", "image/jpeg", outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Something unexpected happened while generating thumbnail: {}", e.getMessage());
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
                    "Something unexpected happened while generating thumbnail");
        }
    }

    private static FileObjectDto generateVideoThumbnailBase64Async(byte[] videoBytes, String fileName)
            throws ResourceException {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return generateVideoThumbnailBase64(videoBytes, fileName);
                } catch (ResourceException e) {
                    throw new RuntimeException(e);
                }
            }).join();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ResourceException) {
                throw (ResourceException) e.getCause();
            }
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
                    "Something unexpected happened while generating thumbnail");
        }
    }

    private static FileObjectDto generateVideoThumbnailBase64(byte[] videoBytes, String fileName)
            throws ResourceException {
        File videoFile = null;
        File thumbnailFile = null;
        File repairedVideoFile = null;
        try {
            log.info("Generating thumbnail for a video");

            // Determine proper suffix based on fileName
            String suffix = ".mp4"; // default
            if (fileName != null && fileName.contains(".")) {
                suffix = fileName.substring(fileName.lastIndexOf("."));
            }
            videoFile = File.createTempFile("video_", suffix);
            Files.write(videoFile.toPath(), videoBytes);
            log.info("Temp video file created at: {}, size: {} bytes", videoFile.getAbsolutePath(), videoFile.length());

            thumbnailFile = File.createTempFile("thumbnail_", ".jpg");

            // First attempt to extract thumbnail
            boolean success = extractThumbnail(videoFile, thumbnailFile);

            // If ffmpeg failed due to moov atom, repair file with faststart
            if (!success) {
                log.warn("Initial thumbnail extraction failed, attempting faststart repair...");
                repairedVideoFile = File.createTempFile("video_repaired_", suffix);

                ProcessBuilder repairPb = new ProcessBuilder(
                        "ffmpeg", "-y", "-i", videoFile.getAbsolutePath(),
                        "-c", "copy", "-movflags", "faststart",
                        repairedVideoFile.getAbsolutePath());
                repairPb.redirectErrorStream(true);
                Process repairProcess = repairPb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(repairProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null)
                        log.info("[ffmpeg-repair] {}", line);
                }
                int repairExit = repairProcess.waitFor();
                if (repairExit != 0 || !repairedVideoFile.exists()) {
                    throw new IOException("Faststart repair failed, exit code: " + repairExit);
                }

                log.info("Faststart repair successful, retrying thumbnail extraction...");
                success = extractThumbnail(repairedVideoFile, thumbnailFile);
                if (!success) {
                    throw new IOException("Thumbnail generation failed even after faststart repair");
                }
            }

            // Read thumbnail file
            byte[] thumbnailBytes = Files.readAllBytes(thumbnailFile.toPath());
            return buildFileObject("video_thumbnail.jpg", "image/jpeg", thumbnailBytes);

        } catch (IOException | InterruptedException e) {
            log.error("Error while generating video thumbnail: {}", e.getMessage(), e);
            throw new ResourceException(ErrorCodes.UNEXPECTED_FAILURE,
                    "Something unexpected happened while generating thumbnail");
        } finally {
            // Cleanup temp files
            if (videoFile != null && videoFile.exists() && !videoFile.delete()) {
                log.warn("Failed to delete temp video file: {}", videoFile.getAbsolutePath());
            }
            if (repairedVideoFile != null && repairedVideoFile.exists() && !repairedVideoFile.delete()) {
                log.warn("Failed to delete repaired temp video file: {}", repairedVideoFile.getAbsolutePath());
            }
            if (thumbnailFile != null && thumbnailFile.exists() && !thumbnailFile.delete()) {
                log.warn("Failed to delete temp thumbnail file: {}", thumbnailFile.getAbsolutePath());
            }
        }
    }

    private static boolean extractThumbnail(File videoFile, File thumbnailFile)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-ss", "00:00:01", "-i", videoFile.getAbsolutePath(),
                "-vframes", "1", thumbnailFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                log.info("[ffmpeg] {}", line);
        }

        int exitCode = process.waitFor();
        log.info("FFmpeg exit code: {}", exitCode);
        return exitCode == 0 && thumbnailFile.exists();
    }

    public static FileObjectDto buildFileObject(String name, String type, byte[] content) {
        FileObjectDto dto = new FileObjectDto();
        dto.setOriginalFilename(name);
        dto.setContentType(type);
        dto.setContent(content);
        return dto;
    }
}
