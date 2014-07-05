package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

/**
 * The low-level shape implementation.
 */
public abstract class Shape {

	/** The VAO object pointer */
	private final int objectPointer;
	
	/** The VBO containing the vertex properties */
	private final int verticesPointer;
	
	/** The VBO containing the vertex indices */
	private final int indicesPointer;
	
	/** The number of indices */
	private final int indicesCount;
	
	/** The shader program */
	private int shaderProgram;
	
	/** The vertex shader */
	private int vertexShader;
	
	/** The fragment shader */
	private int fragmentShader;
	
	
	/**
	 * Creates a shape from a vertex index mapping and a series of vertices.
	 * @param indexArray - the vertex index mapping
	 * @param vertices - the shape's constituent vertices
	 */
	public Shape(byte[] indexArray, Vertex... vertices) {
		// Store the number of indices used to define the shape
		indicesCount = indexArray.length;
		
		// Create a buffer to hold the vertex properties
		FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(
				vertices.length * 8);
		
		// Put the vertex properties into the buffer
		for (Vertex vertex : vertices) {
			verticesBuffer.put(vertex.xyzw());
			verticesBuffer.put(vertex.rgba());
		}
		
		// Flip the buffer
		// This is required by OpenGL
		verticesBuffer.flip();


		// Create a buffer to hold the vertex indices
		ByteBuffer indicesBuffer =
				BufferUtils.createByteBuffer(indexArray.length);

		// Put the vertex indices into the buffer
		indicesBuffer.put(indexArray);

		// Flip the buffer
		// This is required by OpenGL
		indicesBuffer.flip();
		
		
		// Create the vertex array object
		// This represents the shape being constructed
		objectPointer = glGenVertexArrays();
		glBindVertexArray(objectPointer);
		
		
		// Create the vertex buffer object
		// This represents the constituent vectors of the shape being
		// constructed
		verticesPointer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, verticesPointer);
		glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);

		// Put the positions part of the VBO in the attributes list at
		// index 0, and the colours part of the VBO in the attributes
		// list at index 1
		glVertexAttribPointer(0, 4, GL_FLOAT, false, 32, 0);
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 32, 16);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		
		// Clear the VAO
		glBindVertexArray(0);
		
		
		// Create the vertex indices buffer object
		indicesPointer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, indicesPointer);
		glBufferData(GL_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		
		// Setup the colour shaders
		setupShaders();
	}
	
	
	/**
	 * Renders the shape.
	 */
	public void render() {
		// Load the shader program
		glUseProgram(shaderProgram);
		
		// Load the shape
		glBindVertexArray(objectPointer);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);

		// Load the shape's vertex indices
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesPointer);

		// Draw the shape
		glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_BYTE, 0);

		// Clear the VAO, VBO and shader program
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glBindVertexArray(0);
		glUseProgram(0);
	}
	
	/**
	 * Releases the shape's memory.
	 */
	public void destroy() {
		// Detach the program and any shaders
		glUseProgram(0);
		glDetachShader(shaderProgram, vertexShader);
		glDetachShader(shaderProgram, fragmentShader);
		
		// Delete the program and any shaders
		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
		glDeleteProgram(shaderProgram);
		
		// Select the VAO
		glBindVertexArray(objectPointer);
		
		// Disable the VBO index from the VAO attributes list
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		
		// Delete the vertex VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(verticesPointer);
		
		// Delete the vertex properties VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(indicesPointer);
		
		// Delete the VAO
		glBindVertexArray(0);
		glDeleteVertexArrays(objectPointer);
	}
	
	
	/**
	 * Sets up the shape's shaders.
	 */
	private void setupShaders() {
		// Load the shaders
		vertexShader = loadShader("src/graphics/VertexShader.glsl",
				GL_VERTEX_SHADER);
		fragmentShader = loadShader("src/graphics/FragmentShader.glsl",
				GL_FRAGMENT_SHADER);
		
		// Create a new shader program and add the two shaders to it
		shaderProgram = glCreateProgram();
		glAttachShader(shaderProgram, vertexShader);
		glAttachShader(shaderProgram, fragmentShader);

		// Bind the two attribute locations to the shader program
		glBindAttribLocation(shaderProgram, 0, "in_Position");
		glBindAttribLocation(shaderProgram, 1, "in_Color");
		
		// Link and validate the shader program
		glLinkProgram(shaderProgram);
		glValidateProgram(shaderProgram);
	}
	
	
	/**
	 * Loads shaders from GLSL script files.
	 * <p>
	 * GLSL script files are OpenGL shader scripts.
	 * </p>
	 * @param filename - the filename of the shader script,
	 * relative to the project root (an example would be
	 * "src/Shader.glsl")
	 * @param type - the type of shader the script is defining
	 * @return an index to the loaded shader
	 */
	private static int loadShader(String filename, int type) {
		StringBuilder shaderSource = new StringBuilder();
		int shaderID = 0;
		
		try {
			BufferedReader reader =
					new BufferedReader(new FileReader(filename));
			
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		shaderID = glCreateShader(type);
		glShaderSource(shaderID, shaderSource);
		glCompileShader(shaderID);

		return shaderID;
	}

}
