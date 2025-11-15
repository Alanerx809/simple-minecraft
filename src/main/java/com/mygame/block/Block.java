package com.mygame.block;

import org.joml.Vector3f;

public class Block {
    public static final float SIZE = 1.0f;
    private BlockType type;
    private Vector3f position;
    
    public Block(BlockType type, float x, float y, float z) {
        this.type = type;
        this.position = new Vector3f(x, y, z);
    }
    
    public BlockType getType() {
        return type;
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public enum BlockType {
        GRASS(0, 0.8f, 0.4f, 0.1f, 1.0f),  // Green top, brown sides
        DIRT(1, 0.5f, 0.3f, 0.1f, 1.0f),    // Brown
        STONE(2, 0.5f, 0.5f, 0.5f, 1.0f),   // Gray
        BEDROCK(3, 0.1f, 0.1f, 0.1f, 1.0f); // Dark gray
        
        private final int id;
        private final float r, g, b, a;
        
        BlockType(int id, float r, float g, float b, float a) {
            this.id = id;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
        
        public float[] getColor() {
            return new float[]{r, g, b, a};
        }
    }
}
