package com.mazegen.controller;

import com.mazegen.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Debug controller to help analyze edge detection issues
 */
@RestController
@RequestMapping("/debug")
public class DebugController {
    
    private final ImageService imageService;
    
    @Autowired
    public DebugController(ImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * Debug endpoint to visualize edge detection results
     */
    @GetMapping("/edges/{imageId}")
    @ResponseBody
    public byte[] debugEdges(@PathVariable String imageId,
                             @RequestParam(defaultValue = "50") int width,
                             @RequestParam(defaultValue = "50") int height) {
        try {
            boolean[][] edges = imageService.detectEdges(imageId, width, height);
            
            // Create a visual representation of the edges
            BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = edgeImage.createGraphics();
            
            // Fill background with white
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            
            // Draw edges in black
            g2d.setColor(Color.BLACK);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (edges[y][x]) {
                        g2d.fillRect(x, y, 1, 1);
                    }
                }
            }
            g2d.dispose();
            
            // Scale up for better visibility
            BufferedImage scaledImage = new BufferedImage(width * 8, height * 8, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2dScaled = scaledImage.createGraphics();
            g2dScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2dScaled.drawImage(edgeImage, 0, 0, width * 8, height * 8, null);
            g2dScaled.dispose();
            
            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaledImage, "PNG", baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Debug edges error: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
    
    /**
     * Debug endpoint to analyze image characteristics
     */
    @GetMapping("/analyze/{imageId}")
    @ResponseBody
    public String analyzeImage(@PathVariable String imageId) {
        try {
            byte[] imageData = imageService.getImage(imageId);
            if (imageData == null) {
                return "Image not found: " + imageId;
            }
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            
            StringBuilder analysis = new StringBuilder();
            analysis.append("Image Analysis for ID: ").append(imageId).append("\n\n");
            analysis.append("Original Size: ").append(image.getWidth()).append("x").append(image.getHeight()).append("\n");
            analysis.append("Type: ").append(getImageType(image.getType())).append("\n\n");
            
            // Analyze brightness distribution
            int[] histogram = new int[256];
            int width = image.getWidth();
            int height = image.getHeight();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color color = new Color(image.getRGB(x, y));
                    int brightness = (int) (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
                    histogram[brightness]++;
                }
            }
            
            // Find statistics
            int minBright = -1, maxBright = -1;
            long totalBrightness = 0;
            int pixelCount = width * height;
            
            for (int i = 0; i < 256; i++) {
                if (histogram[i] > 0) {
                    if (minBright == -1) minBright = i;
                    maxBright = i;
                    totalBrightness += (long) i * histogram[i];
                }
            }
            
            double avgBrightness = (double) totalBrightness / pixelCount;
            int contrast = maxBright - minBright;
            
            analysis.append("Brightness Statistics:\n");
            analysis.append("  Min: ").append(minBright).append("\n");
            analysis.append("  Max: ").append(maxBright).append("\n");
            analysis.append("  Average: ").append(String.format("%.1f", avgBrightness)).append("\n");
            analysis.append("  Contrast: ").append(contrast).append("\n\n");
            
            // Test edge detection at different sizes
            analysis.append("Edge Detection Test (different sizes):\n");
            int[] testSizes = {20, 30, 50, 80};
            
            for (int size : testSizes) {
                try {
                    boolean[][] edges = imageService.detectEdges(imageId, size, size);
                    int edgeCount = 0;
                    for (boolean[] row : edges) {
                        for (boolean pixel : row) {
                            if (pixel) edgeCount++;
                        }
                    }
                    double edgePercentage = (edgeCount * 100.0) / (size * size);
                    analysis.append("  ").append(size).append("x").append(size).append(": ")
                            .append(edgeCount).append(" edges (")
                            .append(String.format("%.1f%%", edgePercentage)).append(")\n");
                } catch (Exception e) {
                    analysis.append("  ").append(size).append("x").append(size).append(": Error - ")
                            .append(e.getMessage()).append("\n");
                }
            }
            
            // Recommendations
            analysis.append("\nRecommendations:\n");
            if (contrast < 30) {
                analysis.append("- Image has very low contrast. Consider using a higher contrast image.\n");
            }
            if (avgBrightness < 50 || avgBrightness > 200) {
                analysis.append("- Image is very dark or very bright. Try an image with more varied lighting.\n");
            }
            analysis.append("- For best results, use images with clear objects, shapes, or text.\n");
            analysis.append("- Try different maze sizes to see which works best with your image.\n");
            
            return analysis.toString();
            
        } catch (Exception e) {
            return "Error analyzing image: " + e.getMessage();
        }
    }
    
    private String getImageType(int type) {
        return switch (type) {
            case BufferedImage.TYPE_INT_RGB -> "RGB";
            case BufferedImage.TYPE_INT_ARGB -> "ARGB";
            case BufferedImage.TYPE_BYTE_GRAY -> "Grayscale";
            case BufferedImage.TYPE_3BYTE_BGR -> "BGR";
            default -> "Type " + type;
        };
    }
}