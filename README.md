# Image-Based Maze Generator (WIP)

A Spring Boot Application that transforms uploaded images into unique, solvable mazes where the solution path follows the outline of objects detected in the image.

### What is it?

The Image-Based Maze Generator is an innovative web app that bridges computer vision and algorithmic maze generation. Users can upload an image, and the application intelligently analyzes the image to detect edges and object boundaries, then creates a rectangular maze where the solution path follows these detected features

### Real-world Applications

- **Educational tools** for teaching pathfinding algorithms and creating mazes for elementary students
- **Puzzle generation** for games and entertainment
- **Computer vision demonstrations** showing edge detection in action

*** Tech Stack

*** **Backend**
- **Java 21**
- **Spring Boot 3.4.6**
- **Spring Web MVC** for RESTful web services and controllers
- **Sprint Boot Validation** - for input validation and error handling

*** **Frontend**
- **Thymeleaf** - Server-side template engine
- **Tailwind CSS**
- **Vanilla JavaScript**

*** **Build & Deployment**
- **Maven**
- **Embedded Tomcat**
- **Jar Packaging**

### **Project Structure**
```
src/main/java/com/mazegen/
├── MazeGeneratorApplication.java    # Main Spring Boot application
├── controller/
│   ├── MazeController.java          # REST API endpoints
│   ├── WebController.java           # Web page controllers
│   └── DebugController.java         # Development/debugging tools
├── service/
│   ├── ImageService.java            # Image processing and storage
│   └── MazeService.java             # Maze generation algorithms
├── model/
│   ├── MazeRequest.java             # Request data transfer object
│   ├── MazeResponse.java            # Response data transfer object
│   └── Point.java                   # Coordinate representation
└── config/
    └── WebConfig.java               # CORS and web configuration

src/main/resources/
├── application.properties           # Application configuration
└── templates/                       # Thymeleaf HTML templates
    ├── layout.html                  # Base template with navigation
    ├── index.html                   # Image upload page
    ├── configure.html               # Maze configuration
    ├── result.html                  # Generated maze display
    ├── download.html                # File download handler
    └── error.html                   # Error page
```

*** **Design Patterns Used**
- **Factory Methods** for object creation utility
- **Strategy Patten** for edge detection algorithms


## 📄 License

This project is licensed under the MIT License.