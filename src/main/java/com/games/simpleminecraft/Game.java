package com.games.simpleminecraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public class Game {
    private long window;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final String TITLE = "Simple Minecraft Clone";
    
    private Camera camera;
    private World world;
    private ShaderProgram shaderProgram;

    private double lastTime;
    private boolean lmbPrev = false;
    private boolean rmbPrev = false;
    private World.BlockType selectedBlock = World.BlockType.DIRT;

    // HUD buffers
    private int hudVao = 0;
    private int hudPosVbo = 0;
    private int hudColVbo = 0;
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Initialize GLFW
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        
        // Create the window
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, TITLE, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Set up input callbacks
        setupInputCallbacks();
        
        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        
        // Enable v-sync
        GLFW.glfwSwapInterval(1);
        
        // Make the window visible
        GLFW.glfwShowWindow(window);
        
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        
        // Set the clear color
        GL11.glClearColor(0.53f, 0.81f, 0.98f, 0.0f);
        
        // Enable depth testing (no face culling to see faces from both sides)
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        
        // Create camera
        camera = new Camera(WIDTH, HEIGHT);
        camera.setPosition(new Vector3f(World.SIZE_X / 2f, 20f, World.SIZE_Z / 2f));
        
        // Create shader program
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader("#version 330\n" +
                "layout (location=0) in vec3 position;\n" +
                "layout (location=1) in vec4 color;\n" +
                "out vec4 vColor;\n" +
                "uniform mat4 projectionMatrix;\n" +
                "uniform mat4 viewMatrix;\n" +
                "uniform mat4 modelMatrix;\n" +
                "void main() {\n" +
                "   vColor = color;\n" +
                "   gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);\n" +
                "}");
        shaderProgram.createFragmentShader("#version 330\n" +
                "in vec4 vColor;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "   fragColor = vColor;\n" +
                "}");
        shaderProgram.link();
        
        // Create world
        world = new World();

        lastTime = GLFW.glfwGetTime();
    }
    
    private void setupInputCallbacks() {
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(window, true);
            }
            if (action == GLFW.GLFW_PRESS) {
                if (key == GLFW.GLFW_KEY_1) selectedBlock = World.BlockType.DIRT;
                if (key == GLFW.GLFW_KEY_2) selectedBlock = World.BlockType.STONE;
                if (key == GLFW.GLFW_KEY_3) selectedBlock = World.BlockType.WOOD;
            }
        });
        
        // Set up mouse input
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        if (GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
        }
    }
    
    private void loop() {
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!GLFW.glfwWindowShouldClose(window)) {
            double now = GLFW.glfwGetTime();
            float dt = (float) Math.min(0.05, now - lastTime);
            lastTime = now;
            // Poll for window events
            GLFW.glfwPollEvents();
            
            // Update game state
            update(dt);
            
            // Clear the framebuffer
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            // Render the scene
            render();
            
            // Swap the color buffers
            GLFW.glfwSwapBuffers(window);
        }
    }
    
    private void update(float dt) {
        // Mouse look
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);
        camera.rotate((float) xpos[0], (float) ypos[0]);

        // Movement input
        boolean forward = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean back = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        boolean jump = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;

        camera.updatePhysics(world, dt, forward, back, left, right, jump);

        // Mining/Placing
        boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        Vector3f origin = new Vector3f(camera.getPosition());
        Vector3f dir = camera.getForwardVector(new Vector3f());
        Vector3f hit = new Vector3f();
        Vector3f normal = new Vector3f();
        if (!lmbPrev && lmb) {
            if (world.raycast(origin, dir, 6f, hit, normal)) {
                world.setBlock((int) hit.x, (int) hit.y, (int) hit.z, World.BlockType.AIR);
                world.rebuildMesh();
            }
        }
        if (!rmbPrev && rmb) {
            if (world.raycast(origin, dir, 6f, hit, normal)) {
                int px = (int) hit.x + (int) normal.x;
                int py = (int) hit.y + (int) normal.y;
                int pz = (int) hit.z + (int) normal.z;
                world.setBlock(px, py, pz, selectedBlock);
                world.rebuildMesh();
            }
        }
        lmbPrev = lmb;
        rmbPrev = rmb;
    }
    
    private void render() {
        shaderProgram.bind();
        
        // Update projection and view matrices
        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        
        // Render the world
        world.render(shaderProgram);
        
        // Render HUD on top
        renderHUD();

        shaderProgram.unbind();
    }

    private void renderHUD() {
        // Prepare orthographic projection
        Matrix4f ortho = new Matrix4f().ortho(0, WIDTH, HEIGHT, 0, -1, 1);
        shaderProgram.setUniform("projectionMatrix", ortho);
        shaderProgram.setUniform("viewMatrix", new Matrix4f().identity());

        // Disable depth so UI is always on top
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        if (hudVao == 0) {
            hudVao = org.lwjgl.opengl.GL30.glGenVertexArrays();
            hudPosVbo = org.lwjgl.opengl.GL15.glGenBuffers();
            hudColVbo = org.lwjgl.opengl.GL15.glGenBuffers();
        }

        org.lwjgl.opengl.GL30.glBindVertexArray(hudVao);

        // Build simple hotbar (3 slots) and crosshair
        float slotW = 60, slotH = 60;
        float margin = 10;
        float totalW = slotW * 3 + margin * 2;
        float x0 = (WIDTH - totalW) / 2f;
        float y0 = HEIGHT - slotH - 20;

        java.util.ArrayList<Float> pos = new java.util.ArrayList<>();
        java.util.ArrayList<Float> col = new java.util.ArrayList<>();

        // Slots background
        for (int i = 0; i < 3; i++) {
            float x = x0 + i * (slotW + margin);
            addQuad(pos, col, x, y0, slotW, slotH, 0f, 0f, 0f, 0.5f);
        }

        // Selected border (slightly bigger)
        int selIndex = (selectedBlock == World.BlockType.DIRT ? 0 : (selectedBlock == World.BlockType.STONE ? 1 : 2));
        float bx = x0 + selIndex * (slotW + margin) - 3;
        float by = y0 - 3;
        addFrame(pos, col, bx, by, slotW + 6, slotH + 6, 4f, 1f, 1f, 1f, 1f);

        // Crosshair (two thin quads)
        float cx = WIDTH / 2f; float cy = HEIGHT / 2f;
        addQuad(pos, col, cx - 10, cy - 1, 20, 2, 1f, 1f, 1f, 0.9f);
        addQuad(pos, col, cx - 1, cy - 10, 2, 20, 1f, 1f, 1f, 0.9f);

        float[] posArr = toArray(pos);
        float[] colArr = toArray(col);

        org.lwjgl.BufferUtils.createFloatBuffer(1); // ensure class loaded
        java.nio.FloatBuffer pbuf = org.lwjgl.BufferUtils.createFloatBuffer(posArr.length).put(posArr);
        pbuf.flip();
        java.nio.FloatBuffer cbuf = org.lwjgl.BufferUtils.createFloatBuffer(colArr.length).put(colArr);
        cbuf.flip();

        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, hudPosVbo);
        org.lwjgl.opengl.GL15.glBufferData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, pbuf, org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW);
        org.lwjgl.opengl.GL20.glVertexAttribPointer(0, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, 0, 0);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);

        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, hudColVbo);
        org.lwjgl.opengl.GL15.glBufferData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, cbuf, org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW);
        org.lwjgl.opengl.GL20.glVertexAttribPointer(1, 4, org.lwjgl.opengl.GL11.GL_FLOAT, false, 0, 0);
        org.lwjgl.opengl.GL20.glEnableVertexAttribArray(1);

        shaderProgram.setUniform("modelMatrix", new Matrix4f().identity());
        org.lwjgl.opengl.GL11.glDrawArrays(org.lwjgl.opengl.GL11.GL_TRIANGLES, 0, posArr.length / 3);

        org.lwjgl.opengl.GL30.glBindVertexArray(0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void addQuad(java.util.List<Float> pos, java.util.List<Float> col, float x, float y, float w, float h, float r, float g, float b, float a) {
        float z = 0f;
        // two triangles
        addVertex(pos, x, y, z); addColor(col, r, g, b, a);
        addVertex(pos, x, y + h, z); addColor(col, r, g, b, a);
        addVertex(pos, x + w, y + h, z); addColor(col, r, g, b, a);
        addVertex(pos, x + w, y + h, z); addColor(col, r, g, b, a);
        addVertex(pos, x + w, y, z); addColor(col, r, g, b, a);
        addVertex(pos, x, y, z); addColor(col, r, g, b, a);
    }

    private void addFrame(java.util.List<Float> pos, java.util.List<Float> col, float x, float y, float w, float h, float t, float r, float g, float b, float a) {
        // top, bottom, left, right rectangles
        addQuad(pos, col, x, y, w, t, r, g, b, a);
        addQuad(pos, col, x, y + h - t, w, t, r, g, b, a);
        addQuad(pos, col, x, y + t, t, h - 2*t, r, g, b, a);
        addQuad(pos, col, x + w - t, y + t, t, h - 2*t, r, g, b, a);
    }

    private void addVertex(java.util.List<Float> pos, float x, float y, float z) {
        pos.add(x); pos.add(y); pos.add(z);
    }
    private void addColor(java.util.List<Float> col, float r, float g, float b, float a) {
        col.add(r); col.add(g); col.add(b); col.add(a);
    }
    private float[] toArray(java.util.List<Float> list) { float[] arr = new float[list.size()]; for (int i=0;i<arr.length;i++) arr[i]=list.get(i); return arr; }
    
    private void cleanup() {
        // Free the window callbacks and destroy the window
        GLFW.glfwDestroyWindow(window);
        
        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
        
        // Clean up shaders
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
    }
    
    public static void main(String[] args) {
        new Game().run();
    }
}
