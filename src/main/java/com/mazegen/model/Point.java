package com.mazegen.model;

/**
 * Immutable point class representing 2D coordinates in the maze
 * Used for pathfinding, maze generation, and coordinate operations
 */
public class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Static factory method for origin point (0, 0)
     */
    public static Point origin() {
        return new Point(0,0);
    }

    /**
     * Method to create Point from new string "x,y"
     * 
     * @param pointString String in format "x,y"
     * @return Point parsed from string
     * @throws IllegalArgumentException if string format is invalid
     */
    public static Point fromString(String pointString) {
        try {
            String[] parts = pointString.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Point string must be in format 'x,y'");
            }
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            return new Point(x,y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in point string: " + pointString, e);
        }
    }

    /**
     * Calculate Manhattan distance to another point
     * |x1 - x2| + |y1 - y2|
     * 
     * @param other The other point
     * @return Manhattan distance as integer
     */
    public int manhattanDistance(Point other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    /**
     * Calculate Euclidean distance to another point
     * sqrt( (x1-x2)^2 + (y1-y2)^2 )
     * 
     * @param other The other point
     * @return Euclidean distance as double
     */
    public double euclideanDistance(Point other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt((dx * dx) + (dy * dy));
    }
    
    /**
     * Create a new point translated by the given offset
     * 
     * @param dx Horizontal offset
     * @param dy Vertical offset
     * @return New point at (x+dx, y+dy)
     */
    public Point translate(int dx, int dy) {
        return new Point(this.x + dx, this.y + dy);
    }

    /**
     * Create a new point translated by another point's coordinates
     * 
     * @param offset The offset point
     * @returns New point at (x+offset.x, y+offset.y)
     */
    public Point translate(Point offset) {
        return new Point(this.x + offset.x, this.y + offset.y);
    }

    /**
     * Check if this point is adjacent (4-connected) to another Point
     * Points are adjacent if they differ by 1 in exactly one coordinate
     * 
     * @param other The other point
     * @return true If points are adjacent(not diagonal)
     */
    public boolean isAdjacentTo(Point other) {
        int dx = Math.abs(this.x - other.x);
        int dy = Math.abs(this.y - other.y);
        return (dx == 1 && dy == 0) || (dx ==0 && dy == 1);
    }

    /** 
     * Check if this point is within the bounds of a rectangle 
     * 
     * @param width Rectangle width (exclusive)
     * @param height Rectangle height (exclusive)
     * @return true If 0 <= x < width and 0 <= y < hieght
     */
    public boolean isWithinBounds(int width, int height, int margin) {
        return x >= margin && x < width - margin && y >= margin && y < height - margin;
    }

    /**
     * Get array of 4-connected neighbor coordinates
     * 
     * @return Array of 4 neighboring points
     */
    public Point[] getNeighbors() {
        return new Point[] {
            new Point(x, y - 1), // up
            new Point(x + 1, y), // right
            new Point(x, y + 1), // down
            new Point(x - 1, y), // left
        };
    }

    /** Get array of 8-connected neight coordinates
     * 
     * @return Array of 8 neighboring points
     */
    public Point[] getAllNeighbors() {
        return new Point[] {
            new Point(x - 1, y - 1),    // top left
            new Point(x, y - 1),        // top
            new Point(x + 1, y - 1),    // top right
            new Point(x + 1, y),        // right
            new Point(x + 1, y + 1),    // bottom right
            new Point(x, y + 1),        // bottom
            new Point(x - 1, y + 1),    // bottom left
            new Point(x - 1, y),        // left
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return x == point.x && y == point.y;
    }

    @Override
    public String toString() {
        return "Point{x=" + x + ", y=" + y + "}";
    }

    /**
     * Get string representation in "x,y" format
     */
    public String toCoordinateString() {
        return x + "," + y;
    }

}
