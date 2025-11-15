package com.games.simpleminecraft;

import static org.lwjgl.opengl.GL20.*;
import org.joml.Matrix4f;

public class ShaderProgram {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    
    public ShaderProgram() {
        programId = glCreateProgram();
    }
    
    public void createVertexShader(String shaderCode) {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }
    
    public void createFragmentShader(String shaderCode) {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }
    
    private int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }
        
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }
        
        glAttachShader(programId, shaderId);
        
        return shaderId;
    }
    
    public void link() {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader code: " + glGetProgramInfoLog(programId, 1024));
        }
        
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader code: " + glGetProgramInfoLog(programId, 1024));
        }
    }
    
    public void bind() {
        glUseProgram(programId);
    }
    
    public void unbind() {
        glUseProgram(0);
    }
    
    public void cleanup() {
        unbind();
        if (programId != 0) {
            if (vertexShaderId != 0) {
                glDetachShader(programId, vertexShaderId);
            }
            if (fragmentShaderId != 0) {
                glDetachShader(programId, fragmentShaderId);
            }
            glDeleteProgram(programId);
        }
    }
    
    public void setUniform(String name, Matrix4f value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            float[] matrix = new float[16];
            value.get(matrix);
            glUniformMatrix4fv(location, false, matrix);
        }
    }
    
    public int getProgramId() {
        return programId;
    }
}
