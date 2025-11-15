package com.mygame.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private Vector3f position;
    private Vector3f rotation;
    private float moveSpeed = 0.1f;
    private float mouseSensitivity = 0.2f;
    
    public Camera() {
        position = new Vector3f(8, 10, 8); // Start above the ground
        rotation = new Vector3f(30, -45, 0); // Look down at an angle
    }
    
    public void moveForward() {
        position.x += Math.sin(Math.toRadians(rotation.y)) * moveSpeed;
        position.z -= Math.cos(Math.toRadians(rotation.y)) * moveSpeed;
    }
    
    public void moveBackward() {
        position.x -= Math.sin(Math.toRadians(rotation.y)) * moveSpeed;
        position.z += Math.cos(Math.toRadians(rotation.y)) * moveSpeed;
    }
    
    public void moveLeft() {
        position.x -= Math.sin(Math.toRadians(rotation.y - 90)) * moveSpeed;
        position.z += Math.cos(Math.toRadians(rotation.y - 90)) * moveSpeed;
    }
    
    public void moveRight() {
        position.x -= Math.sin(Math.toRadians(rotation.y + 90)) * moveSpeed;
        position.z += Math.cos(Math.toRadians(rotation.y + 90)) * moveSpeed;
    }
    
    public void moveUp() {
        position.y += moveSpeed;
    }
    
    public void moveDown() {
        position.y -= moveSpeed;
    }
    
    public void rotate(float dx, float dy) {
        rotation.y += dx * mouseSensitivity;
        rotation.x += dy * mouseSensitivity;
        
        // Clamp vertical rotation to avoid flipping
        if (rotation.x > 90) rotation.x = 90;
        if (rotation.x < -90) rotation.x = -90;
    }
    
    public Matrix4f getViewMatrix() {
        Matrix4f viewMatrix = new Matrix4f().identity();
        
        // Rotate the view
        viewMatrix.rotateX((float) Math.toRadians(rotation.x))
                 .rotateY((float) Math.toRadians(rotation.y));
        
        // Translate to the camera position
        viewMatrix.translate(-position.x, -position.y, -position.z);
        
        return viewMatrix;
    }
    
    public Vector3f getPosition() {
        return position;
    }
}
