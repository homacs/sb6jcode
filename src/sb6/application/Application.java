package sb6.application;

import java.nio.ByteBuffer;

import org.lwjgl.LWJGLUtil;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import sb6.GLAPIHelper;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*; // for constants like GL_TRUE
import static org.lwjgl.system.MemoryUtil.*;




public abstract class Application extends GLAPIHelper{
	
	public static final boolean DEBUG = true;

	protected Info info = new Info();
	private DebugMessageHandler debugMessageHandler;
	private int exitStatus;
	private GLFWWindowSizeCallback windowSizeCB;
	private GLFWKeyCallback keyCB;
	private GLFWMouseButtonCallback mouseButtonCB;
	private GLFWCursorPosCallback cursorPosCB;
	private GLFWScrollCallback scrollCB;
	private static long window;
	static Application app;

	
	public Application(String windowTitle) {
		info.title = windowTitle;
	}
	
	public void run() {
		boolean running = true;
		app = this;

		if (glfwInit() != GL11.GL_TRUE) {
			System.err.print("Failed to initialize GLFW\n");
			return;
		}

		init();

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, info.majorVersion);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, info.minorVersion);
		
		if (DEBUG) {
			glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);
		}
		
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		glfwWindowHint(GLFW_SAMPLES, info.samples);
		glfwWindowHint(GLFW_STEREO, info.flags.stereo ? GL_TRUE : GL_FALSE);

		glfwWindowHint(GLFW_ALPHA_BITS, 0);
		glfwWindowHint(GLFW_DEPTH_BITS, 32);
		glfwWindowHint(GLFW_STENCIL_BITS, 0);

		if (info.flags.fullscreen) {
			long monitor = glfwGetPrimaryMonitor();
			GLFWvidmode mode = new GLFWvidmode(glfwGetVideoMode(monitor));

			info.setWindowWidth(mode.getWidth());
			info.setWindowHeight(mode.getHeight());
			glfwWindowHint(GLFW_RED_BITS, mode.getRedBits());
			glfwWindowHint(GLFW_GREEN_BITS, mode.getGreenBits());
			glfwWindowHint(GLFW_BLUE_BITS, mode.getBlueBits());

			window = glfwCreateWindow(info.getWindowWidth(), info.getWindowHeight(),
					info.title, monitor, 0);
			glfwSwapInterval(info.flags.vsync ? 1 : 0);
		} else {
			glfwWindowHint(GLFW_RED_BITS, 8);
			glfwWindowHint(GLFW_GREEN_BITS, 8);
			glfwWindowHint(GLFW_BLUE_BITS, 8);

			window = glfwCreateWindow(info.getWindowWidth(), info.getWindowHeight(),
					info.title, 0, 0);
		}

		if (window == NULL) {
			System.err.print("Failed to open window\n");
			glfwTerminate();
			return;
		}

		windowSizeCB = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				Application.this.onResize(width, height);
			}
		};
		glfwSetWindowSizeCallback(window, windowSizeCB);
		
		keyCB = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action,
					int mods) {
				Application.this.onKey(key, action);
			}
		};
		glfwSetKeyCallback(window, keyCB);
		mouseButtonCB = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				Application.this.onMouseButton(button, action);
			}

		};
		glfwSetMouseButtonCallback(window, mouseButtonCB);
		
		cursorPosCB = new GLFWCursorPosCallback() {

			@Override
			public void invoke(long window, double xpos, double ypos) {
				Application.this.onMouseMove(xpos, ypos);
			}
		};
		glfwSetCursorPosCallback(window, cursorPosCB);

		scrollCB = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				Application.this.onMouseWheel(yoffset);
			}

		};
		glfwSetScrollCallback(window, scrollCB);
		if (info.flags.cursor)
			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		else
			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

		info.flags.stereo = (glfwGetWindowAttrib(window, GLFW_STEREO) == 1);

		glfwMakeContextCurrent(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the ContextCapabilities instance and makes the OpenGL
        // bindings available for use.
        GLContext.createFromCurrent();
		
		// loading of extensions is done automatically by the lwjgl library,
		// thus, we dont need w3g.

		if (DEBUG) {
			info("VENDOR: " + glGetString(GL_VENDOR));
			info("VERSION: " + glGetString(GL_VERSION));
			info("RENDERER: " + glGetString(GL_RENDERER));
		}
		// Creates a debug message callback for the context
		// which supports OpenGL43, GL_KHR_debug, GL_ARB_debug_output
		// and GL_AMD_debug_output if supported by the drivers.
		debugMessageHandler = new DebugMessageHandler(GL.getCurrent(), this);
		
		exitStatus = 0;
		try {
		
			startup();
	
			while (running && exitStatus == 0) {
				render(glfwGetTime());
	
				// Both buffers are full, one is displayed and the other waits to be swapped in after sync
				glfwSwapBuffers(window);
				// TODO: triple buffering, would allow rendering ahead of static and predictable objects
				
		        /* Poll for and process events */
		        glfwPollEvents();
	
				if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS){
					running = false;
				} else if (glfwWindowShouldClose(window) == GL_TRUE) {
					running = false;
				}
			}
	
			shutdown();

		} catch (Throwable t) {
			t.printStackTrace();
			exitStatus = -1;
		}

		exit(exitStatus);
	}

	protected void requestExit(int status) {
		exitStatus = status;
		GLFW.glfwWindowShouldClose(window);
	}
	
	/** Immediate program exit */
	protected void exit (int status) {
		debugMessageHandler.release();
		glfwTerminate();
		System.exit(status);
	}

	protected void init() {
		if (info.title == null) info.title = "SuperBible6 Example";
		info.setWindowWidth(800);
		info.setWindowHeight(600);
		if (LWJGLUtil.getPlatform()== LWJGLUtil.Platform.MACOSX) {
			info.majorVersion = 3;
			info.minorVersion = 2;
		} else {
			info.majorVersion = 4;
			info.minorVersion = 3;
		}
		info.samples = 0;
		info.flags.cursor = true;
		info.flags.fullscreen = false;
		info.flags.stereo = false;
		info.flags.vsync = false;
		if (DEBUG) {
			info.flags.debug = true;
		}
	}

	protected abstract void startup() throws Throwable;

	protected abstract void render(double currentTime) throws Throwable;
	
	protected abstract void shutdown() throws Throwable;

	protected void onResize(int w, int h) {
		info.setWindowWidth(w);
		info.setWindowHeight(h);
	}

	protected void onKey(int key, int action) {

	}

	protected void onMouseButton(int button, int action) {

	}

	protected void onMouseMove(double xpos, double ypos) {

	}

	protected void onMouseWheel(double yoffset) {

	}

	void onDebugMessage(int source, int type, int id, int severity,
			int length, String message) {
		fatal(message);
	}

	static Cursor getMousePosition() {
		final ByteBuffer x = ByteBuffer.allocate(4);
		final ByteBuffer y = ByteBuffer.allocate(4);
		glfwGetCursorPos(window, x, y);
		return new Cursor(x.getDouble(), y.getDouble());

	}

	void setVsync(boolean enable) {
		info.flags.vsync = enable;
		glfwSwapInterval(info.flags.vsync ? 1 : 0);
	}

	/**
	 * Print error message and exit abnormal
	 * @param errmsg
	 */
	protected void fatal(String errmsg) {
		throw new Error("FATAL: " + errmsg);
	}

	/** 
	 * Logs an error message on stderr.
	 * @param errmsg
	 */
	protected void error(String errmsg) {
		System.err.println("ERROR: " + errmsg);
	}

	/**
	 * Logs warnmsg on stdout
	 * @param warnmsg
	 */
	protected void warn(String warnmsg) {
		System.out.println("WARN: " + warnmsg);
	}

	/**
	 * Logs infomsg on stdout.
	 * @param infomsg
	 */
	protected void info(String infomsg) {
		System.out.println("INFO: " + infomsg);
	}
	

	public static class Info {
		public String title;
		public int windowWidth;
		public int windowHeight;
		public int majorVersion;
		public int minorVersion;
		public int samples;

		public static class Flags {
			public boolean fullscreen;
			public boolean vsync;
			public boolean cursor;
			public boolean stereo;
			public boolean debug;
		}

		public Flags flags = new Flags();

		public int getWindowWidth() {
			return windowWidth;
		}

		public void setWindowWidth(int windowWidth) {
			this.windowWidth = windowWidth;
		}

		public int getWindowHeight() {
			return windowHeight;
		}

		public void setWindowHeight(int windowHeight) {
			this.windowHeight = windowHeight;
		}

	}

	public static class Cursor {

		private double x;
		private double y;

		public Cursor(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}

	}

	public String getMediaPath() {
		// requires trailing slash!
		return "res/media/";
//		return "../sb6code/bin/media/";
	}


}
