#version 420 core

// Texture object received from application.
// Textures are directly sent to the fragment shader and don't 
// take the detour via the vertex shader like other inputs.
layout (binding = 0) uniform sampler2D tex_object;

//
in VS_OUT
{
	// Texture coordinate corresponding to the fragments location
	// received from vertex and tesselation shaders.
    vec2 tc;
} fs_in;

// Output to framebuffer
out vec4 color;

void main(void)
{
	// Simply read from the texture at the (scaled to (x*3,y*1)) coordinates, and
	// assign the result to the shader's output.
    color = texture(tex_object, fs_in.tc * vec2(3.0, 1.0));
}
