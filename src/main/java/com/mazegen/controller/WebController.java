package com.mazegen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import com.mazegen.model.MazeRequest;
import com.mazegen.model.MazeResponse;
import com.mazegen.service.ImageService;
import com.mazegen.service.MazeService;

/**
 * Web controller for web app interface
 * Handles all web page requests and form submissions
 * 
 * Provides endpoints for:
 * - Main upload page
 * - Image upload processing
 * - Maze generation
 * - Results display
 */
@Controller
public class WebController {
    
    private final ImageService imageService;
    private final MazeService mazeService;

    @Autowired
    public WebController(ImageService imageService, MazeService mazeService) {
        this.imageService = imageService;
        this.mazeService = mazeService;
    }

    /**
     * Display main upload page
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("mazeRequest", new MazeRequest());
        return "index";
    }

    /**
     * Handle image upload and redirect to configuration page
     */
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("Processing file upload: " + file.getOriginalFilename());

            // Validate file
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/";
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "Please upload a valid image file (JPG, PNG, GIF, BMP).");
                return "redirect:/";
            }

            // Check file size (10MB limit)
            if (file.getSize() > 10 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("error", "File size must be less than 10MB");
                return "redirect:/";
            }

            // Store image
            String imageId = imageService.storeImage(file);
            System.out.println("Image stored with ID: " + imageId);

            // Add success message and redirect to configuration
            redirectAttributes.addFlashAttribute("success", "Image uploaded successfully");
            redirectAttributes.addFlashAttribute("imageId", imageId);
            redirectAttributes.addFlashAttribute("filename", file.getOriginalFilename());
            redirectAttributes.addFlashAttribute("fileSize", formatFileSize(file.getSize()));

            return "redirect:/configure?imageId=" + imageId;

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    /**
     * Display maze configuration page
     */
    @GetMapping("/configure")
    public String configure(@RequestParam("imageId") String imageId, Model model, RedirectAttributes redirectAttributes) {
        System.out.println("Configuring maze for image ID: " + imageId);
        
        // Verify image exists
        if (!imageService.imageExists(imageId)) {
            redirectAttributes.addFlashAttribute("error", "Image not found. Please upload an image first");
            return "redirect:/";
        }

        MazeRequest mazeRequest = new MazeRequest(imageId);
        model.addAttribute("mazeRequest", mazeRequest);
        model.addAttribute("imageId", imageId);

        return "configure";
    }
    
    /**
     * Generate maze and display results
     */
    @PostMapping("/generate")
    public String generateMaze(@ModelAttribute("mazeRequest") MazeRequest mazeRequest,
                                Model model, RedirectAttributes redirectAttributes) {
        
        try {
            System.out.println("Starting maze generation for: " + mazeRequest);

            // Validate request
            if (!mazeRequest.isValidDimensions()) {
                redirectAttributes.addFlashAttribute("error", "Invalid maze dimensions. Width and height muse be between 10 and 200");
                return "redirect:/configure?imageId=" + mazeRequest.getImageId();
            }

            // Check for large mazes and warn user
            int totalCells = mazeRequest.getTotalCells();
            if(totalCells > 10000) {
                System.out.println("Warning: Large mage requested (" + totalCells + " cells");
            }

            // Set timeout for maze generation
            long startTime = System.currentTimeMillis();

            // Generate maze
            MazeResponse mazeResponse;
            try {
                mazeResponse = mazeService.generateMaze(mazeRequest);
            } catch (Exception e) {
                System.err.println("Maze generation failed: " + e.getMessage());
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Maze generation failed. Try a smaller size or different image");
                return "redirect:/configure?imageId=" + mazeRequest.getImageId();
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Maze generation complated in " + (endTime - startTime) + "ms");

            if (!mazeResponse.isSuccess()) {
                System.err.println("Maze generation unsuccesful: " + mazeResponse.getMessage());
                redirectAttributes.addFlashAttribute("error", mazeResponse.getMessage());
                return "redirect:/configure?imageId=" + mazeRequest.getImageId();
            }

            // Check if maze is too large for web display
            if (mazeResponse.getMaze().length() > 100000) {
                System.out.println("Warning: Very large maze generated (" + mazeResponse.getMaze().length() + " characters)");
            }

            // Add maze data to model
            model.addAttribute("mazeResponse", mazeResponse);
            model.addAttribute("mazeRequest", mazeRequest);
            model.addAttribute("mazeLines", mazeResponse.getMaze().split("\n"));

            return "result";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate maze: " + e.getMessage());
            return "redirect:/configure?imageId=" + mazeRequest.getImageId();
        }
    }

    /**
     * Display uplaoded image
     */
    @GetMapping("/image/{imageId}")
    @ResponseBody
    public byte[] getImage(@PathVariable String imageId) {
        try {
            return imageService.getImage(imageId);
        } catch (Exception e) {
            System.err.println("Error retrieving image: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Download maze as text file
     */
    @GetMapping("/download/{imageId}")
    public String downloadMaze(@PathVariable String imageId,
                                @RequestParam("width") int width,
                                @RequestParam("height") int height,
                                Model model) {

        try {
            MazeRequest request = new MazeRequest(imageId, width, height);
            MazeResponse response = mazeService.generateMaze(request);

            model.addAttribute("maze", response.getMaze());
            model.addAttribute("filename", "maze_" + width + "x" + height + ".txt");

            return "download";
        
        } catch (Exception e) {
            System.err.println("Download error: " + e.getMessage());
            model.addAttribute("error", "Failed to generate maze for download");
            return "error";
        }
    }

    /**
     * Error page handler
     */
    @GetMapping("/error")
    public String error() {
        return "error";
    }

    /**
     * Debug endpoint
     */
    @GetMapping("/debug/generate")
    @ResponseBody
    public String debugGenerate(@RequestParam String imageId,
                                @RequestParam(defaultValue = "20") int width,
                                @RequestParam(defaultValue = "20") int height) {
        try {
            System.out.println("Debug generation: " + imageId + " " + width + "x" + height);
            MazeRequest request = new MazeRequest(imageId, width, height);
            MazeResponse response = mazeService.generateMaze(request);
            return "Success: " + response.getMessage(); 
        } catch (Exception e) {
            return "Error: " + e.getMessage() + "\nStack: " + java.util.Arrays.toString(e.getStackTrace());
        }
    }
}
