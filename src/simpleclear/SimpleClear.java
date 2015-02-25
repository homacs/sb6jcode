package simpleclear;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import sb6.Application;

public class SimpleClear extends Application {
	FloatBuffer backgroundColor4f;
	
	
	private static final boolean oldVersion = false;

	public SimpleClear () {
		super("Our first OpenGL application");
		backgroundColor4f = BufferUtils.createFloatBuffer(4);
	}
	
	@Override
	protected void startup() {
	}

	@Override
	protected void shutdown() {
	}

	protected void render(double currentTime)
	{
		
		if (oldVersion) {
			// Simply clear the window with red
			glClearColor((float)Math.sin(currentTime) * 0.5f + 0.5f,
					(float)Math.cos(currentTime) * 0.5f + 0.5f,
					0.0f, 1.0f);
			
			glClear(GL_COLOR_BUFFER_BIT);
		} else {
			backgroundColor4f.put(0, (float)Math.sin(currentTime) * 0.5f + 0.5f);
			backgroundColor4f.put(1, (float)Math.cos(currentTime) * 0.5f + 0.5f);
			backgroundColor4f.put(2, 0.f);
			backgroundColor4f.put(3, 1.f);
			glClearBuffer(GL_COLOR, 0, backgroundColor4f);
		}
	}
	
	public static void main(String[] args) {
		new SimpleClear().run();
	}


}
