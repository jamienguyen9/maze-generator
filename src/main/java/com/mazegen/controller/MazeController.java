package com.mazegen.controller;

import com.mazegen.model.MazeRequest;
import com.mazegen.model.MazeResponse;
import com.mazegen.service.ImageService;
import com.mazegen.service.MazeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;


/**
 * REST Controller for maze generation endpoints
 * 
 * Provides endpoints for:
 * - Image upload and sotrage
 * - Maze generated based on uploaded image
 * - Image retrieval
 * 
 * Note: All endpoints should include proper error handling and validation
 */
@RestController
@RequestMapping("/api/maze")
@CrossOrigin(origins = "*")
public class MazeController {
    private final ImageService imageService;
    private final MazeService mazeService;

    @Autowired
    public MazeController(ImageService imageService, MazeService mazeService) {
        this.imageService = imageService;
        this.mazeService = mazeService;
    }

    /**
     * Upload an image file for maze generation
     * Accepts JPG, PNG, GIF, BMP
     * 
     * @param file The uploaded image file
     * @return Response with upload status and image ID
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Store image and get ID
            String imageId = imageService.storeImage(file);
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("imageId", imageId);
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Generate a maze based on an uploaded image
     * The maze solution will follow the outline of objects in the image
     * 
     * @param request Contains image ID and maze parameters
     * @returns Generated maze as ASCII art with metadata
     */
    @PostMapping("/generate")
    public ResponseEntity<MazeResponse> generateMaze(@Valid @RequestBody MazeRequest request) {
        try {
            // Verify image exists
            if (!imageService.imageExists(request.getImageId())) {
                return ResponseEntity.badRequest()
                    .body(new MazeResponse(false, "Image not found with ID: " + request.getImageId(), null, null));
            }

            // Generate Maze
            MazeResponse mazeResponse = mazeService.generateMaze(request);
            return ResponseEntity.ok(mazeResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MazeResponse(false, "Failed to generate maze: " + e.getMessage(), null, null));
        }
    } 

    /**
     * Retrieve an uploaded image by ID
     * 
     * @param imageId the unique image identifier
     * @return The image file as byte array
     */
    @GetMapping("/image/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable String imageId) {
        try {
            byte[] imageData = imageService.getImage(imageId);
            if (imageData == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // Default is JPEG, could be enhanced to detect actual type
                .body(imageData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint to verify service status
     * 
     * @return Simple status response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String>  response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Image Maze Generator");
        response.put("version", "0.0.1");
        return ResponseEntity.ok(response);
    }
}
