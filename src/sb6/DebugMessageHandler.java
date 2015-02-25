package sb6;

import static org.lwjgl.LWJGLUtil.log;
import static org.lwjgl.opengl.AMDDebugOutput.*;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLDebugMessageAMDCallback;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.libffi.Closure;

public class DebugMessageHandler {
	
	private GLContext context;
	private Application app;
	private ByteArrayOutputStream buffer;
	private PrintStream stream;
	private Closure handle;

	public DebugMessageHandler(GLContext context, Application app) {
		this.context = context;
		this.app = app;
		buffer = new ByteArrayOutputStream();
		stream = new PrintStream(buffer);
		handle = setupDebugMessageCallback();
	}
	
	private void forward(int source, int typeOrCategory, int id, int severity, int length) {
		stream.flush();
		app.onDebugMessage(source, typeOrCategory, id, severity, length, buffer.toString());
		buffer.reset();
	}

	public void release() {
		handle.release();
	}
	
	/**
	 * Detects the best debug output functionality to use and creates a callback that prints information to the specified {@link java.io.PrintStream}. The
	 * callback function is returned as a {@link Closure}, that should be {@link Closure#release released} when no longer needed.
	 *
	 * @param stream the output PrintStream
	 */
	public Closure setupDebugMessageCallback() {
		ContextCapabilities capabilities = context.getCapabilities();
		if ( capabilities.OpenGL43 ) {
			log("[GL] Using OpenGL 4.3 for error logging.");
			GLDebugMessageCallback proc = createDEBUGPROC();
			glDebugMessageCallback(proc, NULL);
			return proc;
		}

		if ( capabilities.GL_KHR_debug ) {
			log("[GL] Using KHR_debug for error logging.");
			GLDebugMessageCallback proc = createDEBUGPROC();
			KHRDebug.glDebugMessageCallback(proc, NULL);
			return proc;
		}

		if ( capabilities.GL_ARB_debug_output ) {
			log("[GL] Using ARB_debug_output for error logging.");
			GLDebugMessageARBCallback proc = createDEBUGPROCARB();
			glDebugMessageCallbackARB(proc, NULL);
			return proc;
		}

		if ( capabilities.GL_AMD_debug_output ) {
			log("[GL] Using AMD_debug_output for error logging.");
			GLDebugMessageAMDCallback proc = createDEBUGPROCAMD();
			glDebugMessageCallbackAMD(proc, NULL);
			return proc;
		}

		log("[GL] No debug output implementation is available.");
		return null;
	}

	private static void printDetail(PrintStream stream, String type, String message) {
		stream.printf("\t%s: %s\n", type, message);
	}

	private GLDebugMessageCallback createDEBUGPROC() {
		return new GLDebugMessageCallback() {
			@Override
			public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
				stream.println("[LWJGL] OpenGL debug message");
				printDetail(stream, "ID", String.format("0x%X", id));
				printDetail(stream, "Source", getSource(source));
				printDetail(stream, "Type", getType(type));
				printDetail(stream, "Severity", getSeverity(severity));
				printDetail(stream, "Message", memDecodeUTF8(memByteBuffer(message, length)));
				forward(source, type, id, severity, length);
			}

			private String getSource(int source) {
				switch ( source ) {
					case GL_DEBUG_SOURCE_API:
						return "API";
					case GL_DEBUG_SOURCE_WINDOW_SYSTEM:
						return "WINDOW SYSTEM";
					case GL_DEBUG_SOURCE_SHADER_COMPILER:
						return "SHADER COMPILER";
					case GL_DEBUG_SOURCE_THIRD_PARTY:
						return "THIRD PARTY";
					case GL_DEBUG_SOURCE_APPLICATION:
						return "APPLICATION";
					case GL_DEBUG_SOURCE_OTHER:
						return "OTHER";
					default:
						return getUnknownToken(source);
				}
			}

			private String getType(int type) {
				switch ( type ) {
					case GL_DEBUG_TYPE_ERROR:
						return "ERROR";
					case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
						return "DEPRECATED BEHAVIOR";
					case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
						return "UNDEFINED BEHAVIOR";
					case GL_DEBUG_TYPE_PORTABILITY:
						return "PORTABILITY";
					case GL_DEBUG_TYPE_PERFORMANCE:
						return "PERFORMANCE";
					case GL_DEBUG_TYPE_OTHER:
						return "OTHER";
					case GL_DEBUG_TYPE_MARKER:
						return "MARKER";
					default:
						return getUnknownToken(type);
				}
			}

			private String getSeverity(int severity) {
				switch ( severity ) {
					case GL_DEBUG_SEVERITY_HIGH:
						return "HIGH";
					case GL_DEBUG_SEVERITY_MEDIUM:
						return "MEDIUM";
					case GL_DEBUG_SEVERITY_LOW:
						return "LOW";
					case GL_DEBUG_SEVERITY_NOTIFICATION:
						return "NOTIFICATION";
					default:
						return getUnknownToken(severity);
				}
			}
		};
	}

	private GLDebugMessageARBCallback createDEBUGPROCARB() {
		return new GLDebugMessageARBCallback() {
			@Override
			public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
				stream.println("[LWJGL] ARB_debug_output message");
				printDetail(stream, "ID", String.format("0x%X", id));
				printDetail(stream, "Source", getSource(source));
				printDetail(stream, "Type", getType(type));
				printDetail(stream, "Severity", getSeverity(severity));
				printDetail(stream, "Message", memDecodeUTF8(memByteBuffer(message, length)));
				forward(source, type, id, severity, length);
			}

			private String getSource(int source) {
				switch ( source ) {
					case GL_DEBUG_SOURCE_API_ARB:
						return "API";
					case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB:
						return "WINDOW SYSTEM";
					case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB:
						return "SHADER COMPILER";
					case GL_DEBUG_SOURCE_THIRD_PARTY_ARB:
						return "THIRD PARTY";
					case GL_DEBUG_SOURCE_APPLICATION_ARB:
						return "APPLICATION";
					case GL_DEBUG_SOURCE_OTHER_ARB:
						return "OTHER";
					default:
						return getUnknownToken(source);
				}
			}

			private String getType(int type) {
				switch ( type ) {
					case GL_DEBUG_TYPE_ERROR_ARB:
						return "ERROR";
					case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB:
						return "DEPRECATED BEHAVIOR";
					case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB:
						return "UNDEFINED BEHAVIOR";
					case GL_DEBUG_TYPE_PORTABILITY_ARB:
						return "PORTABILITY";
					case GL_DEBUG_TYPE_PERFORMANCE_ARB:
						return "PERFORMANCE";
					case GL_DEBUG_TYPE_OTHER_ARB:
						return "OTHER";
					default:
						return getUnknownToken(type);
				}
			}

			private String getSeverity(int severity) {
				switch ( severity ) {
					case GL_DEBUG_SEVERITY_HIGH_ARB:
						return "HIGH";
					case GL_DEBUG_SEVERITY_MEDIUM_ARB:
						return "MEDIUM";
					case GL_DEBUG_SEVERITY_LOW_ARB:
						return "LOW";
					default:
						return getUnknownToken(severity);
				}
			}
		};
	}

	private GLDebugMessageAMDCallback createDEBUGPROCAMD() {
		return new GLDebugMessageAMDCallback() {
			@Override
			public void invoke(int id, int category, int severity, int length, long message, long userParam) {
				stream.println("[LWJGL] AMD_debug_output message");
				printDetail(stream, "ID", String.format("0x%X", id));
				printDetail(stream, "Category", getCategory(category));
				printDetail(stream, "Severity", getSeverity(severity));
				printDetail(stream, "Message", memDecodeUTF8(memByteBuffer(message, length)));
				forward(-1, category, id, severity, length);
			}

			private String getCategory(int category) {
				switch ( category ) {
					case GL_DEBUG_CATEGORY_API_ERROR_AMD:
						return "API ERROR";
					case GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD:
						return "WINDOW SYSTEM";
					case GL_DEBUG_CATEGORY_DEPRECATION_AMD:
						return "DEPRECATION";
					case GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD:
						return "UNDEFINED BEHAVIOR";
					case GL_DEBUG_CATEGORY_PERFORMANCE_AMD:
						return "PERFORMANCE";
					case GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD:
						return "SHADER COMPILER";
					case GL_DEBUG_CATEGORY_APPLICATION_AMD:
						return "APPLICATION";
					case GL_DEBUG_CATEGORY_OTHER_AMD:
						return "OTHER";
					default:
						return getUnknownToken(category);
				}
			}

			private String getSeverity(int severity) {
				switch ( severity ) {
					case GL_DEBUG_SEVERITY_HIGH_AMD:
						return "HIGH";
					case GL_DEBUG_SEVERITY_MEDIUM_AMD:
						return "MEDIUM";
					case GL_DEBUG_SEVERITY_LOW_AMD:
						return "LOW";
					default:
						return getUnknownToken(severity);
				}
			}
		};
	}
	
	private static String getUnknownToken(int token) {
		return String.format("Unknown (0x%X)", token);
	}

}
