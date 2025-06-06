package com.mazegen.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for maze generation
 * Contains parameters needed to generate a maze from an uploaded image
 * 
 * Validation Rules:
 * - imageId: Required, cannot be blank
 * - mazeWidth: Between 10 and 200 pixels
 * - mazeHeight: Between 10 and 200 pixels
 * 
 * Default values are set to 50x50 for resonable maze size
 */
public class MazeRequest {
    
    @NotBlank(message = "Image ID is required")
    private String imageId;

    @Min(value = 10, message = "Maze width must be at least 10")
    @Max(value = 200, message = "Maze width cannot exceed 200")
    private int mazeWidth = 50;

    @Min(value = 10, message = "Maze height must be at least 10")
    @Max(value = 200, message = "Maze height cannot exceed 200")
    private int mazeHeight = 50;

    public MazeRequest () {}

    public MazeRequest(String imageId, int mazeWidth, int mazeHeight) {
        this.imageId = imageId;
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
    }

    public MazeRequest(String imageId) {
        this.imageId = imageId;
        // mazeWidth and mazeHeight use default values (50)
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public int getMazeWidth() {
        return mazeWidth;
    }

    public void setMazeWidth(int mazeWidth) {
        this.mazeWidth = mazeWidth;
    }

    public int getMazeHeight() {
        return mazeHeight;
    }

    public void setMazeHeight(int mazeHeight) {
        this.mazeHeight = mazeHeight;
    }

    /**
     * Validates that the maze dimensions are reasonable
     */
    public boolean isValidDimensions() {
        return mazeWidth >= 10 && mazeWidth <= 200 && mazeHeight >= 10 && mazeHeight <= 200; 
    }

    /**
     * Calculate total maze cells for complexity estimation
     */
    public int getTotalCells() {
        return mazeWidth * mazeHeight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MazeRequest that = (MazeRequest) obj;
        return mazeWidth == that.mazeWidth &&
               mazeHeight == that.mazeHeight &&
               imageId != null ? imageId.equals(that.imageId) : that.imageId == null;
    }

    @Override
    public int hashCode() {
        int result = imageId != null ? imageId.hashCode() : 0;
        result = 31 * result + mazeWidth;
        result = 31 * result + mazeHeight;
        return result;
    }

    @Override
    public String toString() {
        return "MazeRequest{" +
               "imageId='" + imageId + '\'' +
               ", mazeWidth=" + mazeWidth +
               ", mazeHeight=" + mazeHeight + 
               ", totalCells=" + getTotalCells() + 
               "}"; 
    }
}
