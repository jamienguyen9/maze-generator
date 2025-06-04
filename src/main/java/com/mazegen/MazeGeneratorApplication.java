package com.mazegen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Maze Generator
 * 
 * This Spring Boot application provides REST endpoints to:
 * 1. Upload images for maze generation
 * 2. Generate rectangular mazes where the solution follows image content outlines
 * 3. Retrieve generated mazes with customizable parameters
 * 
 * This uses Java 21 and Spring Boot 3.4.6
 */
@SpringBootApplication
public class MazeGeneratorApplication {

	
	public static void main(String[] args) {
		SpringApplication.run(MazeGeneratorApplication.class, args);
		System.out.println("Maze Generator started successfully.");
		System.out.println("Access the application at: http://localhost:8080");
		System.out.println("Upload endpoint: POST http://localhost:8080/api/maze/upload");
		System.out.println("Generate Maze endpoint: POST https://localhost:8080/api/maze/generate");
	}

}
