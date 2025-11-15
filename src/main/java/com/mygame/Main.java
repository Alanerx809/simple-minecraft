package com.mygame;

import com.mygame.block.Block;
import com.mygame.render.Camera;
import com.mygame.render.ShaderProgram;
import com.mygame.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
    private long window;
    private int width = 1280;
    private int height = 720;
    private String title = "Simple Minecraft Clone";
    private Camera camera;
    private World world;
    private ShaderProgram shaderProgram;
    private int vaoId;
    private int vboId;
    private Matrix4f projectionMatrix;
    private double lastX, lastY;
    private boolean firstMouse = true;
    
    public void run() {
        init();
        loop();
        
        // Cleanup
        cleanup();
    }
    
    private void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        
        // Create the window
        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Center the window
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(
            window,
            (vidmode.width() - width) / 2,
            (vidmode.height() - height) / 2
        );
        
        // Setup input callbacks
        setupInputCallbacks();
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Make the window visible
        glfwShowWindow(window);
        
        // Initialize OpenGL
        GL.createCapabilities();
        
        // Create camera
        camera = new Camera();
        
        // Create world
        world = new World();
        
        // Initialize shaders
        initShaders();
        
        // Initialize projection matrix
        projectionMatrix = new Matrix4f()
            .setPerspective((float) Math.toRadians(70.0f), (float) width / height, 0.01f, 1000.0f);
        
        // Enable depth testing and face culling
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Set the clear color (sky blue)
        glClearColor(0.53f, 0.81f, 0.98f, 1.0f);
        
        // Hide and capture the cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }
    
    private void setupInputCallbacks() {
        // Key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        // Mouse movement callback
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }
            
            float xoffset = (float) (xpos - lastX);
            float yoffset = (float) (lastY - ypos); // reversed since y-coordinates range from bottom to top
            
            lastX = xpos;
            lastY = ypos;
            
            camera.rotate(xoffset, yoffset);
        });
        
        // Window resize callback
        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            this.width = width;
            this.height = height;
            glViewport(0, 0, width, height);
            projectionMatrix.setPerspective((float) Math.toRadians(70.0f), (float) width / height, 0.01f, 1000.0f);
        });
    }
    
    private void initShaders() {
        try {
            // Load shader source code
            String vertexShaderSource = new String(Files.readAllBytes(Paths.get("src/main/resources/shaders/vertex.vs")));
            String fragmentShaderSource = new String(Files.readAllBytes(Paths.get("src/main/resources/shaders/fragment.fs")));
            
            // Create and link shader program
            shaderProgram = new ShaderProgram();
            shaderProgram.createVertexShader(vertexShaderSource);
            shaderProgram.createFragmentShader(fragmentShaderSource);
            shaderProgram.link();
            
            // Get uniform locations
            int projectionMatrixLoc = glGetUniformLocation(shaderProgram.getProgramId(), "projectionMatrix");
            int viewMatrixLoc = glGetUniformLocation(shaderProgram.getProgramId(), "viewMatrix");
            int modelMatrixLoc = glGetUniformLocation(shaderProgram.getProgramId(), "modelMatrix");
            
            // Set up the projection matrix (this doesn't change per frame)
            shaderProgram.bind();
            FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
            projectionMatrix.get(matrixBuffer);
            glUniformMatrix4fv(projectionMatrixLoc, false, matrixBuffer);
            shaderProgram.unbind();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    private void processInput() {
        // Close window on ESC
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            glfwSetWindowShouldClose(window, true);
        }
        
        // Camera movement
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            camera.moveForward();
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            camera.moveBackward();
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            camera.moveLeft();
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            camera.moveRight();
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            camera.moveUp();
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            camera.moveDown();
        }
    }
    
    private void render() {
        // Clear the framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Use our shader program
        shaderProgram.bind();
        
        // Update view matrix
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        camera.getViewMatrix().get(matrixBuffer);
        int viewMatrixLoc = glGetUniformLocation(shaderProgram.getProgramId(), "viewMatrix");
        glUniformMatrix4fv(viewMatrixLoc, false, matrixBuffer);
        
        // Render all blocks
        for (Block block : world.getBlocks()) {
            renderBlock(block);
        }
        
        // Unbind shader
        shaderProgram.unbind();
    }
    
    private void renderBlock(Block block) {
        float x = block.getPosition().x;
        float y = block.getPosition().y;
        float z = block.getPosition().z;
        float size = Block.SIZE;
        
        float[] vertices = {
            // Front face
            x, y + size, z + size,  // top right
            x, y, z + size,         // bottom right
            x + size, y, z + size,  // bottom left
            x + size, y + size, z + size, // top left
            
            // Back face
            x + size, y + size, z,  // top right
            x + size, y, z,         // bottom right
            x, y, z,                // bottom left
            x, y + size, z,         // top left
            
            // Top face
            x, y + size, z,         // top right
            x, y + size, z + size,  // bottom right
            x + size, y + size, z + size, // bottom left
            x + size, y + size, z,  // top left
            
            // Bottom face
            x, y, z + size,         // top right
            x, y, z,                // bottom right
            x + size, y, z,         // bottom left
            x + size, y, z + size,  // top left
            
            // Left face
            x, y + size, z,         // top right
            x, y, z,                // bottom right
            x, y, z + size,         // bottom left
            x, y + size, z + size,  // top left
            
            // Right face
            x + size, y + size, z + size, // top right
            x + size, y, z + size,  // bottom right
            x + size, y, z,         // bottom left
            x + size, y + size, z   // top left
        };
        
        // Get block color
        float[] color = block.getType().getColor();
        
        // Create color array (same color for all vertices)
        float[] colors = new float[vertices.length / 3 * 4];
        for (int i = 0; i < colors.length / 4; i++) {
            colors[i*4] = color[0];
            colors[i*4+1] = color[1];
            colors[i*4+2] = color[2];
            colors[i*4+3] = color[3];
        }
        
        // Create index array
        int[] indices = new int[36];
        for (int i = 0; i < 6; i++) {
            indices[i*6] = i*4;
            indices[i*6+1] = i*4+1;
            indices[i*6+2] = i*4+2;
            indices[i*6+3] = i*4;
            indices[i*6+4] = i*4+2;
            indices[i*6+5] = i*4+3;
        }
        
        // Create VAO and VBOs
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        int colorVboId = glGenBuffers();
        int eboId = glGenBuffers();
        
        // Bind VAO
        glBindVertexArray(vaoId);
        
        // Position VBO
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        
        // Color VBO
        glBindBuffer(GL_ARRAY_BUFFER, colorVboId);
        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(colors.length);
        colorBuffer.put(colors).flip();
        glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
        
        // EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        int[] indicesArray = indices;
        java.nio.IntBuffer indexBuffer = BufferUtils.createIntBuffer(indicesArray.length);
        indexBuffer.put(indicesArray).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        // Enable vertex attributes
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        
        // Draw the block
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        
        // Cleanup
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        // Delete buffers
        glDeleteBuffers(new int[]{vboId, colorVboId, eboId});
        glDeleteVertexArrays(new int[]{vaoId});
    }
    
    private void loop() {
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            // Process input
            processInput();
            
            // Render the scene
            render();
            
            // Poll for window events
            glfwPollEvents();
            
            // Swap the color buffers
            glfwSwapBuffers(window);
        }
    }
    
    private void cleanup() {
        // Cleanup shaders
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
        
        // Destroy the window
        if (window != 0) {
            glfwDestroyWindow(window);
        }
        
        // Terminate GLFW
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    public static void main(String[] args) {
        new Main().run();
    }
}
