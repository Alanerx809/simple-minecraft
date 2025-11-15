package com.mygame.world;

import com.mygame.block.Block;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World {
    private static final int WORLD_SIZE = 16;
    private static final int WORLD_HEIGHT = 16;
    private List<Block> blocks;
    
    public World() {
        blocks = new ArrayList<>();
        generateTerrain();
    }
    
    private void generateTerrain() {
        Random random = new Random(12345); // Fixed seed for consistent terrain
        
        for (int x = 0; x < WORLD_SIZE; x++) {
            for (int z = 0; z < WORLD_SIZE; z++) {
                // Generate height using simple noise
                int height = (int) (Math.sin(x * 0.2) * 2 + Math.cos(z * 0.2) * 2 + 5);
                
                // Add blocks from bottom to top
                for (int y = 0; y < WORLD_HEIGHT; y++) {
                    if (y == 0) {
                        // Bottom layer is always bedrock
                        blocks.add(new Block(Block.BlockType.BEDROCK, x, y, z));
                    } else if (y < height - 3) {
                        // Stone layer
                        blocks.add(new Block(Block.BlockType.STONE, x, y, z));
                    } else if (y < height - 1) {
                        // Dirt layer
                        blocks.add(new Block(Block.BlockType.DIRT, x, y, z));
                    } else if (y < height) {
                        // Grass layer on top
                        blocks.add(new Block(Block.BlockType.GRASS, x, y, z));
                    }
                    // Air is not stored
                }
            }
        }
    }
    
    public List<Block> getBlocks() {
        return blocks;
    }
}
