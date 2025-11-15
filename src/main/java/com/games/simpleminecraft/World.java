package com.games.simpleminecraft;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class World {
    public static final int SIZE_X = 64;
    public static final int SIZE_Y = 32;
    public static final int SIZE_Z = 64;

    public enum BlockType { AIR, GRASS, DIRT, STONE, WOOD, LEAVES }

    private BlockType[][][] blocks;

    private int vaoId;
    private int vboPosId;
    private int vboColId;
    private int vertexCount;

    public World() {
        generateTerrain();
        rebuildMesh();
    }

    private void generateTerrain() {
        blocks = new BlockType[SIZE_X][SIZE_Y][SIZE_Z];
        Vector3f center = new Vector3f(SIZE_X / 2f, 0, SIZE_Z / 2f);
        float radius = Math.min(SIZE_X, SIZE_Z) * 0.45f;

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                float dx = x - center.x;
                float dz = z - center.z;
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                float mask = 1.0f - Math.min(1.0f, dist / radius);
                if (mask <= 0) continue;
                int baseHeight = (int) (mask * 8) + 8; // island hill
                for (int y = 0; y < baseHeight && y < SIZE_Y; y++) {
                    if (y == baseHeight - 1) setBlock(x, y, z, BlockType.GRASS);
                    else if (y > baseHeight - 4) setBlock(x, y, z, BlockType.DIRT);
                    else setBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        // Place a few simple trees on grass
        int placed = 0;
        for (int x = 4; x < SIZE_X - 4 && placed < 8; x += 7) {
            for (int z = 4; z < SIZE_Z - 4 && placed < 8; z += 9) {
                int y = getTopY(x, z);
                if (y > 0 && getBlock(x, y, z) == BlockType.GRASS) {
                    placeTree(x, y + 1, z);
                    placed++;
                }
            }
        }
    }

    private void placeTree(int x, int y, int z) {
        int trunkH = 4;
        for (int i = 0; i < trunkH; i++) setBlockSafe(x, y + i, z, BlockType.WOOD);
        int leavesY = y + trunkH;
        for (int dy = -2; dy <= 1; dy++) {
            int r = dy == 1 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= r + 1) setBlockSafe(x + dx, leavesY + dy, z + dz, BlockType.LEAVES);
                }
            }
        }
    }

    private int getTopY(int x, int z) {
        for (int y = SIZE_Y - 1; y >= 0; y--) {
            if (blocks[x][y][z] != null && blocks[x][y][z] != BlockType.AIR) return y;
        }
        return -1;
    }

    public BlockType getBlock(int x, int y, int z) {
        if (!inBounds(x, y, z)) return BlockType.AIR;
        BlockType t = blocks[x][y][z];
        return t == null ? BlockType.AIR : t;
    }

    public void setBlock(int x, int y, int z, BlockType t) {
        if (!inBounds(x, y, z)) return;
        blocks[x][y][z] = t;
    }

    private void setBlockSafe(int x, int y, int z, BlockType t) {
        if (inBounds(x, y, z)) blocks[x][y][z] = t;
    }

    public boolean isSolid(int x, int y, int z) {
        BlockType t = getBlock(x, y, z);
        return t != BlockType.AIR && t != BlockType.LEAVES; // leaves non-solid
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public void rebuildMesh() {
        List<Float> pos = new ArrayList<>();
        List<Float> col = new ArrayList<>();

        for (int x = 0; x < SIZE_X; x++)
            for (int y = 0; y < SIZE_Y; y++)
                for (int z = 0; z < SIZE_Z; z++) {
                    BlockType t = getBlock(x, y, z);
                    if (t == BlockType.AIR) continue;
                    float[] c = colorFor(t);
                    addCubeFaces(pos, col, x, y, z, c);
                }

        float[] posArr = toArray(pos);
        float[] colArr = toArray(col);
        vertexCount = posArr.length / 3;

        if (vaoId == 0) vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        if (vboPosId == 0) vboPosId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboPosId);
        FloatBuffer pbuf = BufferUtils.createFloatBuffer(posArr.length);
        pbuf.put(posArr).flip();
        glBufferData(GL_ARRAY_BUFFER, pbuf, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        if (vboColId == 0) vboColId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboColId);
        FloatBuffer cbuf = BufferUtils.createFloatBuffer(colArr.length);
        cbuf.put(colArr).flip();
        glBufferData(GL_ARRAY_BUFFER, cbuf, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void addCubeFaces(List<Float> pos, List<Float> col, int x, int y, int z, float[] c) {
        // 6 faces: only add if neighbor is air/non-solid
        if (!isSolid(x, y, z + 1)) addFace(pos, col, x, y, z, Face.FRONT, c);
        if (!isSolid(x, y, z - 1)) addFace(pos, col, x, y, z, Face.BACK, c);
        if (!isSolid(x - 1, y, z)) addFace(pos, col, x, y, z, Face.LEFT, c);
        if (!isSolid(x + 1, y, z)) addFace(pos, col, x, y, z, Face.RIGHT, c);
        if (!isSolid(x, y + 1, z)) addFace(pos, col, x, y, z, Face.TOP, c);
        if (!isSolid(x, y - 1, z)) addFace(pos, col, x, y, z, Face.BOTTOM, c);
    }

    private enum Face { FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM }

    private void addFace(List<Float> pos, List<Float> col, int x, int y, int z, Face face, float[] c) {
        // two triangles per face, 6 vertices
        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;
        switch (face) {
            case FRONT: // z1
                v(pos, x0,y0,z1); v(pos, x0,y1,z1); v(pos, x1,y1,z1);
                v(pos, x1,y1,z1); v(pos, x1,y0,z1); v(pos, x0,y0,z1);
                break;
            case BACK: // z0
                v(pos, x1,y0,z0); v(pos, x1,y1,z0); v(pos, x0,y1,z0);
                v(pos, x0,y1,z0); v(pos, x0,y0,z0); v(pos, x1,y0,z0);
                break;
            case LEFT: // x0
                v(pos, x0,y0,z0); v(pos, x0,y1,z0); v(pos, x0,y1,z1);
                v(pos, x0,y1,z1); v(pos, x0,y0,z1); v(pos, x0,y0,z0);
                break;
            case RIGHT: // x1
                v(pos, x1,y0,z1); v(pos, x1,y1,z1); v(pos, x1,y1,z0);
                v(pos, x1,y1,z0); v(pos, x1,y0,z0); v(pos, x1,y0,z1);
                break;
            case TOP: // y1, CCW when viewed from above (+Y)
                v(pos, x0,y1,z0); v(pos, x1,y1,z0); v(pos, x1,y1,z1);
                v(pos, x1,y1,z1); v(pos, x0,y1,z1); v(pos, x0,y1,z0);
                break;
            case BOTTOM: // y0, CCW when viewed from below (-Y)
                v(pos, x0,y0,z1); v(pos, x1,y0,z1); v(pos, x1,y0,z0);
                v(pos, x1,y0,z0); v(pos, x0,y0,z0); v(pos, x0,y0,z1);
                break;
        }
        for (int i = 0; i < 6; i++) addColor(col, c);
    }

    private void v(List<Float> a, float x, float y, float z) { a.add(x); a.add(y); a.add(z); }
    private void addColor(List<Float> a, float[] c) { a.add(c[0]); a.add(c[1]); a.add(c[2]); a.add(c[3]); }
    private float[] toArray(List<Float> list) { float[] arr = new float[list.size()]; for (int i=0;i<arr.length;i++) arr[i]=list.get(i); return arr; }

    private float[] colorFor(BlockType t) {
        switch (t) {
            case GRASS: return new float[]{0.4f,0.8f,0.3f,1f};
            case DIRT: return new float[]{0.59f,0.39f,0.2f,1f};
            case STONE: return new float[]{0.6f,0.6f,0.6f,1f};
            case WOOD: return new float[]{0.5f,0.3f,0.1f,1f};
            case LEAVES: return new float[]{0.2f,0.7f,0.2f,1.0f};
            default: return new float[]{1f,1f,1f,1f};
        }
    }

    public void render(ShaderProgram shaderProgram) {
        glBindVertexArray(vaoId);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        Matrix4f modelMatrix = new Matrix4f();
        shaderProgram.setUniform("modelMatrix", modelMatrix);

        glDrawArrays(GL_TRIANGLES, 0, vertexCount);

        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        if (vboPosId != 0) glDeleteBuffers(vboPosId);
        if (vboColId != 0) glDeleteBuffers(vboColId);
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
    }

    // Raycast utility from origin along dir, returns hit block position and normal.
    public boolean raycast(Vector3f origin, Vector3f dir, float maxDist, Vector3f hit, Vector3f hitNormal) {
        // 3D DDA grid traversal
        Vector3f pos = new Vector3f((float)Math.floor(origin.x), (float)Math.floor(origin.y), (float)Math.floor(origin.z));
        int stepX = dir.x > 0 ? 1 : -1;
        int stepY = dir.y > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;
        float tMaxX = intBound(origin.x, dir.x);
        float tMaxY = intBound(origin.y, dir.y);
        float tMaxZ = intBound(origin.z, dir.z);
        float tDeltaX = stepX / dir.x;
        float tDeltaY = stepY / dir.y;
        float tDeltaZ = stepZ / dir.z;
        float dist = 0f;
        while (dist <= maxDist) {
            int ix = (int) pos.x, iy = (int) pos.y, iz = (int) pos.z;
            if (inBounds(ix, iy, iz) && isSolid(ix, iy, iz)) {
                hit.set(ix, iy, iz);
                return true;
            }
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) { pos.x += stepX; dist = tMaxX; tMaxX += tDeltaX; if (hitNormal != null) hitNormal.set(-stepX,0,0);} 
                else { pos.z += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ; if (hitNormal != null) hitNormal.set(0,0,-stepZ);} 
            } else {
                if (tMaxY < tMaxZ) { pos.y += stepY; dist = tMaxY; tMaxY += tDeltaY; if (hitNormal != null) hitNormal.set(0,-stepY,0);} 
                else { pos.z += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ; if (hitNormal != null) hitNormal.set(0,0,-stepZ);} 
            }
        }
        return false;
    }

    private float intBound(float s, float ds) {
        if (ds > 0) return (float)((Math.floor(s + 1) - s) / ds);
        if (ds < 0) return (float)((s - Math.floor(s)) / -ds);
        return Float.POSITIVE_INFINITY;
    }
}
