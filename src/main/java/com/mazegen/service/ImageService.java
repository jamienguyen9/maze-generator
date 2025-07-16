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

        System.out.println("Stored image: " + file.getOriginalFilename());

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
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
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
     * Count total number of edge pixels
     */
    private int countEdges(boolean[][] edges) {
        int count = 0;
        for(boolean[] row: edges) {
            for (boolean pixel : row) {
                if (pixel) count++;
            }
        }
        return count;
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

        System.out.println("Starting edge detection for image: " + imageId);
        System.out.println("Target dimensions: " + targetWidth + "x" + targetHeight);

        // Load and resize image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        System.out.println("Original image size: " + image.getWidth() + "x" + image.getHeight());

        BufferedImage resizedImage = resizeImage(image, targetWidth, targetHeight);
        System.out.println("Resized to: " + resizedImage.getWidth() + "x" + resizedImage.getHeight());

        // Convert to graysale and detect edges
        boolean [][] edges = new boolean[targetWidth][targetHeight];
        int edgeCount = 0;
        int totalPixels = targetWidth * targetHeight;

        // Analyze image characteristics first
        analyzeImageCharacteristics(resizedImage);

        // Multi-threshold edge detection
        for (int y = 1; y < targetHeight - 1; y++) {
            for (int x = 1; x < targetWidth - 1; x++) {
                if (isEdgePixel(resizedImage, x, y)) {
                    edges[y][x] = true;
                    edgeCount++;
                }
            }
        }

        System.out.println("Detected " + edgeCount + "edge pixels out of " + totalPixels + "total pizels");
        System.out.println("Edge density: " + String.format("%.2f%%", (edgeCount * 100.0) / totalPixels));
        
        // If very few edges detected, try more aggressive approach
        if (edgeCount < totalPixels *.02) {
            System.out.println("Low edge count detected, trying more aggressive approach...");
            edges = detectEdgesAgressive(resizedImage, targetWidth, targetHeight);
            edgeCount = countEdges(edges);
            System.out.println("Agressive detection found " + edgeCount + " edge pixels");
        }

        // If still no edges, create some artificial structure
        if (edgeCount < 10) {
            System.out.println("Very few edges found, creating artificial structure...");
            edges = createArtificialStructure(targetWidth, targetHeight);
            edgeCount = countEdges(edges);
            System.out.println("Created artificial structure with " + edgeCount + " pixels");
        }

        return edges;
    }

    /** 
     * Analyze image characteristics to help with debugging 
     */
    private void analyzeImageCharacteristics(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int minBrightness = 255;
        int maxBrightness = 0;
        long totalBrightness = 0;
        int pixelCount = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int brightness = getBrightness(image.getRGB(x, y));
                minBrightness = Math.min(minBrightness, brightness);
                maxBrightness = Math.max(maxBrightness, brightness);
                totalBrightness += brightness;
            }
        }

        double avgBrightness = (double) totalBrightness / pixelCount;
        int contrast = maxBrightness - minBrightness;

        System.out.println("Image Analysis: ");
        System.out.println("  Min Brightness: " + minBrightness);
        System.out.println("  Max Brightness: " + maxBrightness);
        System.out.println("  Avg Brightness: " + String.format("%.1f", avgBrightness));
        System.out.println("  Contrast range: " + contrast);

        if (contrast < 50) {
            System.out.println("  WARNING: Low contrast range - may have few detectable edges");
        }
    }

    /** 
     * Method for edge detection
     * 
     * @param image The buffered Image
     * @param x The x-coordinate of the pixel
     * @param y The y-coordinate of the pixel
     * @return Whether the pixel is an edge
     */
    private boolean isEdgePixel(BufferedImage image, int x, int y) {
        // Get brightness values for 3x3 neighborhood
        int topBrightness = getBrightness(image.getRGB(x,  - 1));
        int bottomBrightness = getBrightness(image.getRGB(x, y + 1));
        int leftBrightness = getBrightness(image.getRGB( - 1, y));
        int rightBrightness = getBrightness(image.getRGB(x + 1, y));

        // Sobel edge detection
        int gx = Math.abs(rightBrightness - leftBrightness);
        int gy = Math.abs(bottomBrightness - topBrightness);
        int gradientMagnitude = gx + gy;

        int threshold = calculateAdaptiveThreshold(image, x, y);

        return gradientMagnitude > threshold;
    }

    /** 
     * Method to calculate the adaptive threshold based on local image characteristics
     * 
     * @param image The buffered Image
     * @param x The x-coordinate of the pixel
     * @param y The y-coordinate of the pixel
     * @return adaptive threshold value
     */
    private int calculateAdaptiveThreshold(BufferedImage image, int x, int y) {
        int minBright = 255;
        int maxBright = 0;

        for(int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                    int brightness = getBrightness(image.getRGB(nx, ny));
                    minBright = Math.min(minBright, brightness);
                    maxBright = Math.max(maxBright, brightness);
                }
            }
        }

        int localContrast = maxBright - minBright;

        if (localContrast > 100) return 40;
        if (localContrast > 50) return 25;
        return 15;
    }

    /**
     * More aggressive edge detection method for low contrast images
     * 
     * @param image The buffered Image
     * @param width The width of the image
     * @param height The height of the image
     * @return edge matrix
     */
    private boolean[][] detectEdgesAgressive(BufferedImage image, int width, int height) {
        boolean[][] edges = new boolean[height][width];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int centerBrightness = getBrightness(image.getRGB(x, y));

                // Check all 8 directions
                int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
                int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
                
                int maxDiff = 0;
                for (int i = 0; i < 8; i++) {
                    int nx = x + dx[i];
                    int ny = y + dy[i];
                    
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        int neighborBrightness = getBrightness(image.getRGB(nx, ny));
                        int diff = Math.abs(centerBrightness - neighborBrightness);
                        maxDiff = Math.max(maxDiff, diff);
                    }
                }

                edges[y][x] = maxDiff > 10;
            }
        }
        return edges;
    }

    /**
     * Create artificial structure when no edges are detected
     */
    private boolean[][] createArtificialStructure(int width, int height) {
        boolean[][] edges = new boolean[height][width];

        // Create a simple cross pattern
        int centerX = width / 2;
        int centerY = height / 2;

        // Horizontal line
        for (int x = width / 4; x < 3 * width / 4; x++) {
            if (centerY >= 0 && centerY < height) {
                edges[centerY][x] = true;
            }
        }
        
        // Vertical line
        for (int y = height / 4; y < 3 * height / 4; y++) {
            if (centerX >= 0 && centerX < width) {
                edges[y][centerX] = true;
            }
        }
        
        // Add some random scattered points
        for (int i = 0; i < Math.min(20, width * height / 50); i++) {
            int x = 1 + (int)(Math.random() * (width - 2));
            int y = 1 + (int)(Math.random() * (height - 2));
            edges[y][x] = true;
        }
        
        System.out.println("Created artificial cross pattern with scattered points");
        
        return edges;
    }

}
