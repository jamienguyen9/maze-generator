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
            // Don't overwrite START or END markers
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

            int edgeCount = countDetectedEdges(edges);
            System.out.println("Detected " + edgeCount + " edge pixels for guidance");



            // Create maze grid
            char[][] maze = generateTraditionalMaze(request.getMazeWidth(), request.getMazeHeight());

            // Add start and end points
            Point start = new Point(1, 1);
            Point end = new Point(request.getMazeWidth() - 2, request.getMazeHeight()- 2);
            maze[start.y][start.x] = START;
            maze[end.y][end.x] = END;

            // Ensure start and end are accessible
            ensurePointAccessible(maze, start);
            ensurePointAccessible(maze, end);

            // Find solution path
            List<Point> solutionPath = findEdgeGuidedSolution(maze, edges, start, end);

            // If no edge-guided solution found, ensure basic connectivity
            if (solutionPath.isEmpty()) {
                System.out.println("No edge-guided path found, creating basic solution");
                solutionPath = ensureBasicSolution(maze, start, end);
            }

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
     * Heuristic function for A* (Manhattan distance).
     */
    private double heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
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
     * Generate a traditional maze using recursive backtracking algorithm.
     * This creates a proper maze with dead ends and multiple paths.
     */
    private char[][] generateTraditionalMaze(int width, int height) {
        System.out.println("Creating traditional maze structure...");
        
        char[][] maze = new char[height][width];
        
        // Initialize with all walls
        for (int y = 0; y < height; y++) {
            Arrays.fill(maze[y], WALL);
        }
        
        // Use recursive backtracking to create maze
        // Start from a random odd position to ensure proper maze structure
        int startX = 1 + (random.nextInt((width - 2) / 2)) * 2;
        int startY = 1 + (random.nextInt((height - 2) / 2)) * 2;
        
        recursiveBacktrack(maze, startX, startY, width, height);
        
        // Add some additional connections to make it less sparse
        addExtraConnections(maze, width, height);
        
        return maze;
    }
    
    /**
     * Recursive backtracking maze generation algorithm.
     */
    private void recursiveBacktrack(char[][] maze, int x, int y, int width, int height) {
        maze[y][x] = PATH;
        
        // Create list of directions (up, right, down, left)
        List<int[]> directions = Arrays.asList(
            new int[]{0, -2}, new int[]{2, 0}, new int[]{0, 2}, new int[]{-2, 0}
        );
        Collections.shuffle(directions, random);
        
        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            
            // Check bounds
            if (newX > 0 && newX < width - 1 && newY > 0 && newY < height - 1) {
                // Check if cell hasn't been visited
                if (maze[newY][newX] == WALL) {
                    // Carve path between current cell and new cell
                    maze[y + dir[1] / 2][x + dir[0] / 2] = PATH;
                    // Recursively visit new cell
                    recursiveBacktrack(maze, newX, newY, width, height);
                }
            }
        }
    }

    /**
     * Add extra connections to make the maze less sparse and more interesting.
     */
    private void addExtraConnections(char[][] maze, int width, int height) {
        int connections = Math.min(width * height / 50, 20);
        
        for (int i = 0; i < connections; i++) {
            int x = 1 + random.nextInt(width - 2);
            int y = 1 + random.nextInt(height - 2);
            
            // Only add connection if it creates interesting branching
            if (maze[y][x] == WALL && shouldAddConnection(maze, x, y, width, height)) {
                maze[y][x] = PATH;
            }
        }
    }
    
    /**
     * Check if adding a connection at this point would be beneficial.
     */
    private boolean shouldAddConnection(char[][] maze, int x, int y, int width, int height) {
        int adjacentPaths = 0;
        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};
        
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            
            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                if (maze[ny][nx] == PATH) {
                    adjacentPaths++;
                }
            }
        }
        
        // Add connection if it connects different path networks
        return adjacentPaths >= 2 && adjacentPaths <= 3 && random.nextDouble() < 0.3;
    }

    /**
     * Ensure a specific point is accessible by carving paths around it.
     */
    private void ensurePointAccessible(char[][] maze, Point point) {
        maze[point.y][point.x] = PATH;
        
        // Ensure at least one adjacent cell is a path
        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};
        
        boolean hasAdjacentPath = false;
        for (int i = 0; i < 4; i++) {
            int nx = point.x + dx[i];
            int ny = point.y + dy[i];
            
            if (isValidCoordinate(nx, ny, maze[0].length, maze.length)) {
                if (maze[ny][nx] == PATH) {
                    hasAdjacentPath = true;
                    break;
                }
            }
        }
        
        // If no adjacent path, create one
        if (!hasAdjacentPath) {
            for (int i = 0; i < 4; i++) {
                int nx = point.x + dx[i];
                int ny = point.y + dy[i];
                
                if (isValidCoordinate(nx, ny, maze[0].length, maze.length)) {
                    maze[ny][nx] = PATH;
                    break;
                }
            }
        }
    }

    /**
     * Find solution path that follows detected edges when possible.
     */
    private List<Point> findEdgeGuidedSolution(char[][] maze, boolean[][] edges, Point start, Point end) {
        System.out.println("Finding edge-guided solution path...");
        
        // Use A* with edge preference and maze structure awareness
        PriorityQueue<PathNode> openSet = new PriorityQueue<>((a, b) -> 
            Double.compare(a.fScore, b.fScore));
        Set<Point> closedSet = new HashSet<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        Map<Point, Double> gScore = new HashMap<>();
        
        gScore.put(start, 0.0);
        openSet.offer(new PathNode(start, heuristic(start, end)));
        
        int[] dx = {0, 1, 0, -1}; // Only 4 directions for cleaner paths
        int[] dy = {-1, 0, 1, 0};
        
        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            Point currentPoint = current.point;
            
            if (currentPoint.equals(end)) {
                List<Point> path = reconstructPath(cameFrom, currentPoint);
                System.out.println("Found edge-guided solution with " + path.size() + " steps");
                return path;
            }
            
            closedSet.add(currentPoint);
            
            for (int i = 0; i < 4; i++) {
                int nx = currentPoint.x + dx[i];
                int ny = currentPoint.y + dy[i];
                Point neighbor = new Point(nx, ny);
                
                if (!isValidCoordinate(nx, ny, maze[0].length, maze.length)) continue;
                if (closedSet.contains(neighbor)) continue;
                if (maze[ny][nx] == WALL) continue; // Can't move through walls
                
                // Calculate movement cost with edge bonus
                double moveCost = 1.0;
                double edgeBonus = 0;
                
                // Strong preference for edge pixels (cat outline)
                if (edges[ny][nx]) {
                    edgeBonus = -0.8; // Significant bonus for following edges
                }
                
                // Small bonus for being near edges
                else if (isNearEdge(edges, nx, ny)) {
                    edgeBonus = -0.2;
                }
                
                double tentativeGScore = gScore.getOrDefault(currentPoint, Double.MAX_VALUE) + moveCost + edgeBonus;
                
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, currentPoint);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, end);
                    openSet.offer(new PathNode(neighbor, fScore));
                }
            }
        }
        
        return new ArrayList<>(); // No path found
    }
    
    /**
     * Check if a point is near an edge pixel.
     */
    private boolean isNearEdge(boolean[][] edges, int x, int y) {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        
        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            
            if (isValidCoordinate(nx, ny, edges[0].length, edges.length)) {
                if (edges[ny][nx]) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Ensure basic solution exists by carving a path if needed.
     */
    private List<Point> ensureBasicSolution(char[][] maze, Point start, Point end) {
        // Use simple BFS to find any path
        Queue<Point> queue = new LinkedList<>();
        Set<Point> visited = new HashSet<>();
        Map<Point, Point> parent = new HashMap<>();
        
        queue.offer(start);
        visited.add(start);
        
        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};
        
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            
            if (current.equals(end)) {
                return reconstructPath(parent, current);
            }
            
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                Point neighbor = new Point(nx, ny);
                
                if (!isValidCoordinate(nx, ny, maze[0].length, maze.length)) continue;
                if (visited.contains(neighbor)) continue;
                if (maze[ny][nx] == WALL) {
                    // Carve through wall if necessary to ensure connectivity
                    maze[ny][nx] = PATH;
                }
                
                visited.add(neighbor);
                parent.put(neighbor, current);
                queue.offer(neighbor);
            }
        }
        
        // If still no path, create a direct line
        return createDirectPath(start, end);
    }
    
    /**
     * Create a direct path between two points.
     */
    private List<Point> createDirectPath(Point start, Point end) {
        List<Point> path = new ArrayList<>();
        int x = start.x;
        int y = start.y;
        
        path.add(new Point(x, y));
        
        while (x != end.x || y != end.y) {
            if (x < end.x) x++;
            else if (x > end.x) x--;
            else if (y < end.y) y++;
            else if (y > end.y) y--;
            
            path.add(new Point(x, y));
        }
        
        return path;
    }
    
    /**
     * Check if coordinates are valid.
     */
    private boolean isValidCoordinate(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
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