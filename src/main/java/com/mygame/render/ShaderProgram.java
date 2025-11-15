package com.mygame.render;

import static org.lwjgl.opengl.GL20.*;

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
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error creating shader: " + glGetShaderInfoLog(shaderId));
        }
        
        glAttachShader(programId, shaderId);
        return shaderId;
    }
    
    public void link() {
        glLinkProgram(programId);
        
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error linking shader program: " + glGetProgramInfoLog(programId));
        }
        
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Warning validating shader code: " + glGetProgramInfoLog(programId));
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
            glDeleteProgram(programId);
        }
    }
    
    public int getProgramId() {
        return programId;
    }
}
