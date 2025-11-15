package com.games.simpleminecraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position;
    private final Vector3f rotation;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f projectionViewMatrix;
    
    private float lastMouseX = 0;
    private float lastMouseY = 0;
    private boolean firstMouse = true;

    // Physics
    private final Vector3f velocity = new Vector3f();
    private boolean onGround = false;
    private static final float PLAYER_WIDTH = 0.6f;   // total width
    private static final float PLAYER_HEIGHT = 1.8f;  // total height (feet to head)
    private static final float EYE_HEIGHT = 1.6f;     // eye above feet
    private static final float EPS = 0.001f;
    private static final float MOVE_SPEED = 6.0f;     // m/s
    private static final float JUMP_SPEED = 5.5f;     // m/s
    private static final float GRAVITY = 16.0f;       // m/s^2

    public Camera(int width, int height) {
        position = new Vector3f(0, 0, 0);
        rotation = new Vector3f(0, 0, 0);
        
        // Set up projection matrix (perspective)
        float aspectRatio = (float) width / height;
        projectionMatrix = new Matrix4f()
            .perspective((float) Math.toRadians(70.0f), aspectRatio, 0.1f, 1000.0f);
            
        viewMatrix = new Matrix4f();
        projectionViewMatrix = new Matrix4f();
        updateViewMatrix();
    }
    
    public void moveForward(float distance) {
        position.x += (float) Math.sin(Math.toRadians(rotation.y)) * distance;
        position.z -= (float) Math.cos(Math.toRadians(rotation.y)) * distance;
    }
    
    public void moveBackward(float distance) {
        position.x -= (float) Math.sin(Math.toRadians(rotation.y)) * distance;
        position.z += (float) Math.cos(Math.toRadians(rotation.y)) * distance;
    }
    
    public void moveLeft(float distance) {
        position.x -= (float) Math.sin(Math.toRadians(rotation.y - 90)) * distance;
        position.z += (float) Math.cos(Math.toRadians(rotation.y - 90)) * distance;
    }
    
    public void moveRight(float distance) {
        position.x -= (float) Math.sin(Math.toRadians(rotation.y + 90)) * distance;
        position.z += (float) Math.cos(Math.toRadians(rotation.y + 90)) * distance;
    }
    
    public void moveUp(float distance) {
        position.y += distance;
    }
    
    public void moveDown(float distance) {
        position.y -= distance;
    }
    
    public void rotate(float mouseX, float mouseY) {
        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
            return;
        }
        
        float xOffset = mouseX - lastMouseX;
        float yOffset = lastMouseY - mouseY; // Reversed since y-coordinates range from bottom to top
        
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        
        float sensitivity = 0.1f;
        xOffset *= sensitivity;
        yOffset *= sensitivity;
        
        rotation.y += xOffset;
        rotation.x += yOffset;
        
        // Constrain the pitch
        if (rotation.x > 89.0f) {
            rotation.x = 89.0f;
        }
        if (rotation.x < -89.0f) {
            rotation.x = -89.0f;
        }
        
        // Update view matrix
        updateViewMatrix();
    }
    
    private void updateViewMatrix() {
        viewMatrix.identity()
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateY((float) Math.toRadians(rotation.y))
            .translate(-position.x, -position.y, -position.z);
    }
    
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    
    public Matrix4f getProjectionViewMatrix() {
        return projectionViewMatrix.set(projectionMatrix).mul(viewMatrix);
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public Vector3f getRotation() {
        return rotation;
    }

    // New helpers for gameplay
    public void setPosition(Vector3f p) {
        this.position.set(p);
        updateViewMatrix();
    }

    public Vector3f getForwardVector(Vector3f out) {
        float yaw = (float) Math.toRadians(rotation.y);
        float pitch = (float) Math.toRadians(rotation.x);
        float cx = (float) (Math.cos(pitch) * Math.sin(yaw));
        float cy = (float) (-Math.sin(pitch));
        float cz = (float) (-Math.cos(pitch) * Math.cos(yaw));
        return out.set(cx, cy, cz).normalize();
    }

    public void updatePhysics(World world, float dt, boolean forward, boolean back, boolean left, boolean right, boolean jump) {
        // Build desired horizontal move direction from yaw
        Vector3f fwd = getForwardVector(new Vector3f());
        fwd.y = 0; fwd.normalize();
        Vector3f rightV = new Vector3f(-fwd.z, 0, fwd.x); // perpendicular on XZ (right-handed)
        Vector3f wishDir = new Vector3f();
        if (forward) wishDir.add(fwd);
        if (back) wishDir.sub(fwd);
        if (left) wishDir.sub(rightV);
        if (right) wishDir.add(rightV);
        if (wishDir.lengthSquared() > 0) wishDir.normalize().mul(MOVE_SPEED);

        // Smooth set horizontal velocity, keep vertical component
        velocity.x = wishDir.x;
        velocity.z = wishDir.z;

        // Gravity and Jump
        velocity.y -= GRAVITY * dt;
        if (jump && onGround) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }

        // Integrate and collide axis by axis (AABB)
        moveAndCollide(world, velocity.x * dt, 0, 0);
        moveAndCollide(world, 0, velocity.y * dt, 0);
        moveAndCollide(world, 0, 0, velocity.z * dt);

        // If touching ground, clamp small negative velocity
        if (onGround && velocity.y < 0) velocity.y = 0;

        updateViewMatrix();
    }

    private void moveAndCollide(World world, float dx, float dy, float dz) {
        if (dx == 0 && dy == 0 && dz == 0) return;
        position.add(dx, dy, dz);

        // Player AABB extents (camera position is at eye)
        float halfW = PLAYER_WIDTH * 0.5f;
        float feetY = position.y - EYE_HEIGHT;
        float minX = position.x - halfW;
        float maxX = position.x + halfW;
        float minY = feetY;
        float maxY = feetY + PLAYER_HEIGHT;
        float minZ = position.z - halfW;
        float maxZ = position.z + halfW;

        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.floor(maxX);
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.floor(maxY);
        int z0 = (int) Math.floor(minZ);
        int z1 = (int) Math.floor(maxZ);

        boolean collided = false;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (world.isSolid(x, y, z)) {
                        collided = true;
                        // Resolve penetration along moved axis only
                        if (dx > 0) position.x = x - halfW - EPS; // block minX - halfW
                        if (dx < 0) position.x = x + 1 + halfW + EPS;
                        if (dz > 0) position.z = z - halfW - EPS;
                        if (dz < 0) position.z = z + 1 + halfW + EPS;
                        if (dy > 0) { // hit head on block bottom at y
                            float newFeet = y - PLAYER_HEIGHT - EPS;
                            position.y = newFeet + EYE_HEIGHT;
                            velocity.y = 0;
                        }
                        if (dy < 0) { // landed on top of block (y+1)
                            float newFeet = y + 1 + EPS;
                            position.y = newFeet + EYE_HEIGHT;
                            velocity.y = 0; onGround = true;
                        }
                    }
                }
            }
        }
        if (!collided && dy < 0) onGround = false; // falling
    }
}
