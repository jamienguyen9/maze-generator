package com.mazegen.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mazegen.model.MazeRequest;
import com.mazegen.model.MazeResponse;
import com.mazegen.model.Point;

import java.io.IOException;
import java.util.*;

/**
 * Service for generating mazes based on image content
 * 
 * The maze generation algorithm:
 * 1. Detects edges in the uploaded image
 * 2. Creates a rectangular maze gric
 * 3. Carves paths that follow the detected edges
 * 4. Ensures there's a valid solution from start to end
 * 5. Adds additional random paths for complexity
 */
@Service
public class MazeService {
    private final ImageService imageService;
    private final Random random = new Random();

    // Maze cell types
    private static final char WALL = '█';
    private static final char PATH = ' ';
    private static final char START = 'S';
    private static final char END = 'E';
    private static final char SOLUTION = '.';

    @Autowired
    public MazeService(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Create initial maze grid filled with walls
     * 
     * @param width Maze width
     * @param height Maze height
     * @return 2D character array representing the maze
     */
    private char[][] createMazeGrid(int width, int height) {
        char[][] maze = new char[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                maze[y][x] = WALL;
            }
        }
        return maze;
    }

    /**
     * Carve paths in the maze based on detected image edges
     * @param edges Detected edges from image processing
     * @return List of points representing the carved path
     */
    private List<Point> carveImagePath(char[][] maze, boolean[][] edges) {
        List<Point> path = new ArrayList<>();
        int height = maze.length;
        int width = maze[0].length;

        // find edge points and create paths
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (edges[y][x]) {
                    maze[y][x] = PATH;
                    path.add(new Point(x, y));

                    connectNearbyEdges(maze, edges, x, y);
                }
            }
        }
        return path;
    }

    /**
     * Connect nearby edge points to create continuous paths
     * 
     * @param maze The maze grid
     * @param edges Edge detection results
     * @param x Current x coordinate
     * @param y Current y coordinate
     */
    private void connectNearbyEdges(char[][] maze, boolean[][] edges, int x, int y) {
        int height = maze.length;
        int width = maze[0].length;

        // check 8 directions for nearby edges
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (nx > 0 && nx < width - 1 && ny > 0 && ny < height - 1) {
                if (edges[ny][nx] && maze[ny][nx] == WALL) {
                    maze[ny][nx] = PATH;
                }
            }
        }
    }

    /**
     * Ensure the maze has connectivity from start to end
     * Adds additional paths if necessary
     * 
     * @param maze The maze grid
     * @return imagePath The path carved from image edges
     */
    private void ensureConnectivity(char[][] maze, List<Point> imagePath) {
        int height = maze.length;
        int width = maze[0].length;

        int additionalPaths = Math.min(50, width * height / 20);

        for (int i = 0; i < additionalPaths; i++) {
            int x = 1 + random.nextInt(width - 2);
            int y = 1 + random.nextInt(height - 2);

            if (maze[y][x] == WALL && random.nextDouble() < 0.3) {
                maze[y][x] = PATH;
            }
        }

        // Ensure start and end areas are accessible
        ensureAreaAccessible(maze, 1, 1, 3);
        ensureAreaAccessible(maze, width - 2, height - 2, 3);
    }

    /**
     * Ensure an area around a point is accessible
     * 
     * @param maze The maze grid
     * @param centerX Center x coordinate
     * @param centerY Center y coordinate
     * @param radius Radius around the center to clear
     */
    private void ensureAreaAccessible(char[][] maze, int centerX, int centerY, int radius) {
        int height = maze.length;
        int width = maze[0].length;

        for (int y = Math.max(1, centerY - radius); y <= Math.min(height - 2, centerY + radius); y++) {
            for (int x = Math.max(1, centerX - radius); x <= Math.min(width - 2, centerX + radius); x++) {
                if (maze[y][x] == WALL) {
                    maze[y][x] = PATH;
                }
            }
        }
    }

    /**
     * Find solution path from start to end using BFS
     * 
     * @param maze The maze grid
     * @param start Start point
     * @param end  End point
     * @return List of points representing the solution path
     */
    private List<Point> findSolutionPath(char[][] maze, Point start, Point end) {
        int height = maze.length;
        int width = maze[0].length;

        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> parent = new HashMap<>();
        Set<Point> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);

        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.equals(end)) {
                // Reconstruct path
                List<Point> path = new ArrayList<>();
                Point p = current;
                while(p != null) {
                    path.add(p);
                    p = parent.get(p);
                }
                Collections.reverse(path);
                return path;
            }

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                Point next = new Point(nx, ny);

                if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited.contains(next) && 
                    (maze[ny][nx] == PATH || maze[ny][nx] == START || maze[ny][nx] == END)) {

                    visited.add(next);
                    parent.put(next, current);
                    queue.offer(next);
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Mark solution path in the maze
     * 
     * @param maze The maze grid
     * @param solutionPath The solution path points
     */
    private void markSolutionPath(char[][] maze, List<Point> solutionPath) {
        for (Point p : solutionPath) {
            if (maze[p.y][p.x] == PATH) {
                maze[p.y][p.x] = SOLUTION;
            }
        }
    }

    /**
     * Convert maze grid to string representation
     * 
     * @param maze The maze grid
     * @return String representation of the maze
     */
    private String mazeToString(char[][] maze) {
        StringBuilder sb = new StringBuilder();
        for (char[] row: maze) {
            for (char cell : row) {
                sb.append(cell);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Create metadata about the generated maze
     * 
     * @param request Original request parameters
     * @param imagePathLength Length of path derived from image
     * @param solutionPathLength Length of solution path
     * @return Map containing maze metadata
     */
    private Map<String, Object> createMetadata(MazeRequest request, int imagePathLength, int solutionPathLength) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", request.getMazeWidth());
        metadata.put("height", request.getMazeHeight());
        metadata.put("imageId", request.getImageId());
        metadata.put("imagePathLength", imagePathLength);
        metadata.put("solutionPathLength", solutionPathLength);
        metadata.put("difficulty", calculateDifficulty(request.getMazeWidth(), request.getMazeHeight(), solutionPathLength));
        metadata.put("generatedAt", System.currentTimeMillis());
        return metadata;
    }

    /**
     * Calculate maze difficulty based on size and solution length
     * 
     * @param width Maze width
     * @param height Maze height
     * @param solutionLength Solution path length
     * @return Diffulty rating (Easy, Medium, Hard, Expert)
     */
    private String calculateDifficulty(int width, int height, int solutionLength) {
        int totalCells = width * height;
        double complexity = (double) solutionLength / totalCells;

        if (totalCells < 500 && complexity < 0.3) return "Easy";
        if (totalCells < 1000 && complexity < 0.5) return "Medium";
        if (totalCells < 2000 && complexity < 0.7) return "Hard";
        return "Expert";
    }


    /** 
     * Generate a maze based on the provided request parameters
     * 
     * @param request contains image Id and maze generation settings
     * @return Complete maze response with ASCII representation and metadata
     */
    public MazeResponse generateMaze(MazeRequest request) {
        try {
            // Detect edges int he image
            boolean[][] edges = imageService.detectEdges(
                request.getImageId(),
                request.getMazeWidth(),
                request.getMazeHeight()
            );

            // Create maze grid
            char[][] maze = createMazeGrid(request.getMazeWidth(), request.getMazeHeight());

            // Carve paths based on image eddges
            List<Point> imagePath = carveImagePath(maze, edges);

            // Ensure connectivity and add random paths
            ensureConnectivity(maze, imagePath);

            // Add start and end points
            Point start = new Point(1, 1);
            Point end = new Point(request.getMazeWidth() - 2, request.getMazeHeight()- 2);
            maze[start.y][start.x] = START;
            maze[end.y][end.x] = END;

            // Find and mark solution path
            List<Point> solutionPath = findSolutionPath(maze, start, end);
            markSolutionPath(maze, solutionPath);

            // Convert to string representation
            String mazeString = mazeToString(maze);

            // Create metadata response
            Map<String, Object> metadata = createMetadata(request, imagePath.size(), solutionPath.size());

            return new MazeResponse(true, "Maze generated successfully", mazeString, metadata);

        } catch (IOException e) {
            return new MazeResponse(false, "Failed to process image: " + e.getMessage(), null, null);
        } catch (Exception e) {
            return new MazeResponse(false, "Failed to generate maze: " + e.getMessage(), null, null);
        }
    }
}