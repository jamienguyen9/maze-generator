package com.mazegen.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;


/**
 * Service for handling image upload, storage, and processing operations
 * 
 * Features: 
 * - Image file storage with unique IDs
 * - Image format validation
 * - Edge detection for maze path generation
 * - Memory-based storage (room for improvement here)
 */
@Service
public class ImageService {
    
    // In-memory storage for uploaded images
    private final Map<String, byte[]> imageStorage = new ConcurrentHashMap<>();
    private final Map<String, String> imageMetadata = new ConcurrentHashMap<>();


    /**
     * Store and uploaded image and return a unique identifier
     * 
     * @param file The uploaded image file
     * @return Unique image ID for later retrieval
     * @throws IOException if the file processing fails
     */
    public String storeImage(MultipartFile file) throws IOException {
        String imageId = UUID.randomUUID().toString();

        // Store image data
        byte[] imageData = file.getBytes();
        imageStorage.put(imageId, imageData);

        // Store metadata
        imageMetadata.put(imageId, file.getOriginalFilename());

        return imageId;
    }

    /**
     * Check if an image exists in storage
     * 
     * @param imageId The image identifier
     * @return true if image exists, false otherwise
     */
    public boolean imageExists(String imageId) {
        return imageStorage.containsKey(imageId);
    }

    /**
     * Retrieve stored image data
     * 
     * @param imageId The image identifier
     * @return Image data as byte array, or null if not found
     */
    public byte[] getImage(String imageId) {
        return imageStorage.get(imageId);
    }

    /**
     * Resize image to target dimensions
     * 
     * @param original Original image
     * @param targetWidth Target width
     * @param targetHeight Target height
     * @return Resized image
     */
    private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Calculate birhgtness of an RGB color value
     * 
     * @param rgb RGB color value
     * @return Brightness value (0-255)
     */
    private int getBrightness(int rgb) {
        Color color = new Color(rgb);
        return (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
    }


    /**
     * Process image to detect edges and create a pth for maze generation.
     * Uses simple maze detection algorith to find object boundaries
     * 
     * @param imageId The image identifier
     * @param targetWidth Desired width for processing
     * @param targetHeight Desired height for processing
     * @return 2D boolean array where true represents edges/boundaries
     * @throws IOException If image processing fails
     */
    public boolean[][] detectEdges(String imageId, int targetWidth, int targetHeight) throws IOException {
        byte[] imageData = getImage(imageId);
        if (imageData == null) {
            throw new IOException("Image not found: " + imageId);
        }

        // Load and resize image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        BufferedImage resizedImage = resizeImage(image, targetWidth, targetHeight);

        // Convert to graysale and detect edges
        boolean [][] edges = new boolean[targetWidth][targetHeight];

        for(int y = 1; y < targetHeight - 1; y++) {
            for (int x = 1; x < targetWidth - 1; x++) {
                // Use brightness differences to detect edges
                int centerBrightness = getBrightness(resizedImage.getRGB(x, y));
                int topBrightness = getBrightness(resizedImage.getRGB(x, y - 1));
                int bottomBrightness = getBrightness(resizedImage.getRGB(x, y + 1));
                int leftBrightness = getBrightness(resizedImage.getRGB(x - 1, y));
                int rightBrightness = getBrightness(resizedImage.getRGB(x + 1, y));
                
                // Calculate gradient magnitude
                int horizontalGradient = Math.abs(rightBrightness - centerBrightness) + 
                                        Math.abs(centerBrightness + leftBrightness);
                int verticalGradient = Math.abs(bottomBrightness - centerBrightness) + 
                                        Math.abs(centerBrightness - topBrightness);
                int gradientMagnitude = horizontalGradient + verticalGradient;

                // Adjustable threshold for edge detection
                edges[y][x] = gradientMagnitude > 100;
            }
        }

        return edges;
    }


}
