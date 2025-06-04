package com.mazegen.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Response DTO for maze generation results
 * Contains the generated maze, metadata, and operation status
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MazeResponse {

    private boolean success;
    private String message;
    private String maze;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;

    public MazeResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for successful responses
    public MazeResponse(boolean success, String message, String maze, Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.maze = maze;
        this.metadata = metadata;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for error responses (no maze data)
    public MazeResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.maze = null;
        this.metadata = null;
        this.timestamp = LocalDateTime.now();
    }

    public static MazeResponse success (String maze, Map<String, Object> metadata) {
        return new MazeResponse(true, "Maze generated successfully", maze, metadata);
    }

    public static MazeResponse error(String errorMessage) {
        return new MazeResponse(false, errorMessage);
    }

    public static MazeResponse imageNotFound(String imageId) {
        return new MazeResponse(false, "Image not found with ID: " + imageId);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMaze() {
        return maze;
    }

    public void setMaze(String maze) {
        this.maze = maze;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get formatted timestamp for display purposes
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    /**
     * Check if response contains maze data
     */
    public boolean hasMaze() {
        return maze != null && !maze.trim().isEmpty();
    }

    /**
     * Get maze preview for quick inspection
     */
    public String getMazePreview() {
        if (maze == null) return null;

        String[] lines = maze.split("\n");
        StringBuilder preview = new StringBuilder();
        int previewLines = Math.min(5, lines.length);

        for (int i = 0; i < previewLines; i++) {
            preview.append(lines[i]);
            if (i < previewLines - 1) preview.append("\n");
        }

        if (lines.length > 5) {
            preview.append("\n... (").append(lines.length - 5).append(" more lines)");
        }
        
        return preview.toString();
    }

    /**
     * Get maze dimensions from metadata if available
     */
    public String getMazeDimensions() {
        if (metadata == null) return "Unknown";

        Object width = metadata.get("width");
        Object height = metadata.get("height");

        if (width != null && height != null) {
            return width + "x" + height;
        }

        return "Unknown";
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MazeResponse that = (MazeResponse) obj;
        return success == that.success &&
               message != null ? message.equals(that.message) : that.message == null &&
               maze != null ? maze.equals(that.maze) : that.maze == null;
    }

    @Override
    public int hashCode() {
        int result = success ? 1 : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (maze != null ? maze.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MazeResponse{" +
               "success=" + success + 
               ", message='" + message + '\'' +
               ", maze='" + (maze != null ? 
                    (maze.length() > 100 ? maze.substring(0, 100) + "... (" + maze.length() + " chars)" : maze)
                    : "null") + '\'' + 
               ", metadata=" + metadata + 
               ", timestamp=" + (timestamp != null ? getFormattedTimestamp() : "null") +
               ", dimensions=" + getMazeDimensions() +
               "}";
    }
}
