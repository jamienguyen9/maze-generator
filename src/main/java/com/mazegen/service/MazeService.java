package com.mazegen.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${maze.max-width:100}")
    private int maxWidth;

    @Value("${maze.max-height:100}")
    private int maxHeight;

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

        // Fill with walls
        for (int y = 0; y < height; y++) {
            Arrays.fill(maze[y], WALL);
        }

        createMainCorridors(maze, width, height);

        addRandomOpenings(maze, width, height);

        for (int x = 0; x < width; x++) {
            maze[0][x] = WALL;
            maze[height - 1][x] = WALL;
        }
        for(int y = 0; y < height; y++) {
            maze[y][0] = WALL;
            maze[y][width - 1] = WALL;
        }

        return maze;
    }

    /**
     * Create main corridors to ensure path connectivity
     * 
     *  @param maze 2D character array representing the maze
     * @param width width of the maze
     * @param height height of the maze
     */
    private void createMainCorridors(char[][] maze, int width, int height) {
        int midY = height / 2;
        for(int x = 1; x < width - 1; x++) {
            maze[midY][x] = PATH;
        }

        int midX = width / 2;
        for(int y = 1; y < height - 1; y++) {
            maze[y][midX] = PATH;
        }

        for(int i = 1; i < Math.min(width - 1, height - 1); i ++) {
            if (i < width - 1 && i < height - 1) {
                maze[i][i] = PATH;
            }
            if (i < width - 1 && (height - 1 - i) > 0) {
                maze[height - 1 - i][i] = PATH;
            }
        }
    }

    /**
     * Adds random openings for maze complexity
     * 
     * @param maze 2D character array representing the maze
     * @param width width of the maze
     * @param height height of the maze
     */
    private void addRandomOpenings(char[][] maze, int width, int height) {
        int openings = Math.min(width * height / 8, 200);

        for (int i = 0; i < openings; i++) {
            int x = 1 + random.nextInt(width - 2);
            int y = 1 + random.nextInt(height - 2);

            if (random.nextDouble() < 0.4) {
                maze[y][x] = PATH;
            }
        }
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

        // Make sure path points are accessible
        for (Point p : solutionPath) {
            if (maze[p.y][p.x] == WALL) {
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
        int height = maze.length;
        int width = maze[0].length;

        StringBuilder sb = new StringBuilder(height * (width+ 1));

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(maze[y][x]);
            }
            if (y < height - 1) {
                sb.append('\n');
            }
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
            if (!validateMazeSize(request)) {
                return MazeResponse.error("Maze size too large. Maximum allowed: " + maxWidth + "x" + maxHeight);
            }

            System.out.println("Generating maze: " + request.getMazeWidth() + "x" + request.getMazeHeight());

            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory();
            long estMemory = estimateMemoryUsage(request.getMazeWidth(), request.getMazeHeight());

            System.out.println("Free memory: " + (freeMemory / 1024 / 1024) + "MB, Estimated needed: " + (estMemory / 1024 / 1024) + "MB");

            if (estMemory > freeMemory * 0.8) {
                return MazeResponse.error("Not enough memory for maze of this size. Try a smaller dimension");
            }

            // Detect edges int he image
            boolean[][] edges = imageService.detectEdges(
                request.getImageId(),
                request.getMazeWidth(),
                request.getMazeHeight()
            );


            // Create maze grid
            char[][] maze = createMazeGrid(request.getMazeWidth(), request.getMazeHeight());

            // Add start and end points
            Point start = new Point(1, 1);
            Point end = new Point(request.getMazeWidth() - 2, request.getMazeHeight()- 2);
            maze[start.y][start.x] = START;
            maze[end.y][end.x] = END;

            // Find solution path
            List<Point> solutionPath = createEdgeBasedSolutionPath(maze, edges, start, end);

            int edgeCount = countDetectedEdges(edges);

            edges = null;
            System.gc();

            // Mark solution path in the maze
            markSolutionPath(maze, solutionPath);

            // Convert to string representation
            String mazeString = mazeToString(maze);

            maze = null;
            System.gc();

            // Create metadata response
            Map<String, Object> metadata = createMetadata(request, edgeCount, solutionPath.size());

            return new MazeResponse(true, "Maze generated successfully", mazeString, metadata);

        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory error: " + e.getMessage());
            System.gc();
            return MazeResponse.error("Out of memory. Please try a smaller size");
        } catch (IOException e) {
            return new MazeResponse(false, "Failed to process image: " + e.getMessage(), null, null);
        } catch (Exception e) {
            System.err.println("Maze generation error: " + e.getMessage());
            e.printStackTrace();
            return new MazeResponse(false, "Failed to generate maze: " + e.getMessage(), null, null);
        }
    }

    /**
     * Create a path with bias toward edges if direct path finding fails
     * 
     * @param maze 2D character representation of the maze
     * @param edges maze edges
     * @param start start point of the maze
     * @param end end point of the maze
     * @return
     */
    private List<Point> createEdgeBasedSolutionPath(char[][] maze, boolean[][] edges, Point start, Point end) {
        System.out.println("Creating edge-based solution path...");

        List<Point> edgeGuidedPath = findEdgeGuidedPath(maze, edges, start, end);

        if (!edgeGuidedPath.isEmpty()) {
            System.out.println("Created edge-guided path with " + edgeGuidedPath.size() + " points");
            return edgeGuidedPath;
        }

        System.out.println("Creating fallbackpath with edge bias...");
        return createEdgeBiasedPath(maze, edges, start, end);
    }

    /**
     * Find a path that follows detected edges when possible
     */
    private List<Point> findEdgeGuidedPath(char[][] maze, boolean[][] edges, Point start, Point end) {
        int width = maze[0].length;
        int height = maze.length;
        
        // Use A* algorithm with edge preference
        PriorityQueue<PathNode> openSet = new PriorityQueue<>((a, b) -> 
            Double.compare(a.fScore, b.fScore));
        Set<Point> closedSet = new HashSet<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        Map<Point, Double> gScore = new HashMap<>();
        
        // Initialize start node
        gScore.put(start, 0.0);
        openSet.offer(new PathNode(start, heuristic(start, end)));
        
        int[] dx = {0, 1, 0, -1, 1, -1, 1, -1}; // 8 directions
        int[] dy = {-1, 0, 1, 0, -1, -1, 1, 1};
        
        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            Point currentPoint = current.point;
            
            if (currentPoint.equals(end)) {
                // Reconstruct path
                return reconstructPath(cameFrom, currentPoint);
            }
            
            closedSet.add(currentPoint);
            
            // Explore neighbors
            for (int i = 0; i < 8; i++) {
                int nx = currentPoint.x + dx[i];
                int ny = currentPoint.y + dy[i];
                Point neighbor = new Point(nx, ny);
                
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                if (closedSet.contains(neighbor)) continue;
                
                // Calculate cost with edge preference
                double moveCost = (i < 4) ? 1.0 : 1.414; // Diagonal movement cost
                double edgeBonus = edges[ny][nx] ? -0.5 : 0; // Prefer edges
                double tentativeGScore = gScore.getOrDefault(currentPoint, Double.MAX_VALUE) + moveCost + edgeBonus;
                
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, currentPoint);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, end);
                    openSet.offer(new PathNode(neighbor, fScore));
                }
            }
        }
        
        return new ArrayList<>(); 
    }

    /**
     * Create a path with bias towards edges if direct path finding fails
     */
    private List<Point> createEdgeBiasedPath(char[][] maze, boolean[][] edges, Point start, Point end) {
        List<Point> path = new ArrayList<>();
        
        // Create a simple path first
        path.add(start);
        
        Point current = start;
        int maxSteps = (maze[0].length + maze.length) * 2; // Prevent infinite loops
        int steps = 0;
        
        while (!current.equals(end) && steps < maxSteps) {
            Point next = findNextPointTowardTarget(current, end, edges);
            
            // Avoid going back immediately
            if (path.size() > 1 && next.equals(path.get(path.size() - 2))) {
                // Try alternative direction
                next = findAlternativePoint(current, end, edges);
            }
            
            path.add(next);
            current = next;
            steps++;
        }
        
        // Ensure we end at the target
        if (!current.equals(end)) {
            path.add(end);
        }
        
        return path;
    }

    /**
     * Heuristic function for A* (Manhattan distance).
     */
    private double heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    /**
     * Find next point moving toward target with edge preference.
     */
    private Point findNextPointTowardTarget(Point current, Point target, boolean[][] edges) {
        int width = edges[0].length;
        int height = edges.length;
        
        // Calculate direction toward target
        int dx = Integer.compare(target.x, current.x);
        int dy = Integer.compare(target.y, current.y);
        
        // Try preferred direction with edge bias
        Point preferred = new Point(current.x + dx, current.y + dy);
        if (isValidPoint(preferred, width, height)) {
            return preferred;
        }
        
        // Try adjacent points, preferring edges
        List<Point> candidates = new ArrayList<>();
        int[] directions = {0, 1, 0, -1};
        int[] directionsDy = {-1, 0, 1, 0};
        
        for (int i = 0; i < 4; i++) {
            Point candidate = new Point(current.x + directions[i], current.y + directionsDy[i]);
            if (isValidPoint(candidate, width, height)) {
                candidates.add(candidate);
            }
        }
        
        // Sort by edge preference and distance to target
        candidates.sort((a, b) -> {
            boolean aOnEdge = edges[a.y][a.x];
            boolean bOnEdge = edges[b.y][b.x];
            
            if (aOnEdge && !bOnEdge) return -1;
            if (!aOnEdge && bOnEdge) return 1;
            
            // If both or neither on edge, prefer closer to target
            double distA = Math.sqrt(Math.pow(a.x - target.x, 2) + Math.pow(a.y - target.y, 2));
            double distB = Math.sqrt(Math.pow(b.x - target.x, 2) + Math.pow(b.y - target.y, 2));
            return Double.compare(distA, distB);
        });
        
        return candidates.isEmpty() ? current : candidates.get(0);
    }
    
    /**
     * Find alternative point when direct path is blocked.
     */
    private Point findAlternativePoint(Point current, Point target, boolean[][] edges) {
        int width = edges[0].length;
        int height = edges.length;
        
        // Try perpendicular directions
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        
        for (int i = 0; i < 4; i++) {
            Point candidate = new Point(current.x + dx[i], current.y + dy[i]);
            if (isValidPoint(candidate, width, height)) {
                return candidate;
            }
        }
        
        return current; // Stay in place if no alternatives
    }
    
    /**
     * Check if point is within valid bounds.
     */
    private boolean isValidPoint(Point p, int width, int height) {
        return p.x >= 1 && p.x < width - 1 && p.y >= 1 && p.y < height - 1;
    }

    /**
     * Reconstruct path from A* algorithm.
     */
    private List<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        List<Point> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    /** 
     * Validate Maze size against configured limits and memory contraints 
    */
    private boolean validateMazeSize(MazeRequest request) {
        int width = request.getMazeWidth();
        int height = request.getMazeHeight();

        if (width > maxWidth || height > maxHeight) {
            return false;
        }

        int totalCells = width * height;
        if (totalCells > 10000) {
            return false;
        }
        return true;
    }

    /**
     * Estimate memory usage for a maze of given dimensions
     */
    private long estimateMemoryUsage(int width, int height) {
        long cellsMem = (long) width * height * 2;
        long edgesMem = (long) width * height;
        long pathMem = width * height * 24;
        long strMem = (long) width * height * 2;

        return (cellsMem + edgesMem + pathMem + strMem) * 2;
    }

    /**
     * Count the number of directed edge pixels
     */
    private int countDetectedEdges(boolean[][] edges) {
        int count = 0;
        for (boolean[] row : edges) {
            for (boolean pixel : row) {
                if (pixel) count++;
            }
        }
        return count;
    }

    /** 
     * Helper class for A* Pathfinding
     */
    private static class PathNode {
        Point point;
        double fScore;

        PathNode(Point point, double fScore) {
            this.point = point;
            this.fScore = fScore;
        }
    }
}