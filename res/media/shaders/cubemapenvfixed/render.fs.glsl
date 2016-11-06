#version 410 core

uniform samplerCube tex_cubemap;

in VS_OUT
{
    vec3 normal;
    vec3 view;
    flat mat3 reverse_matrix;
} fs_in;

out vec4 color;

/*
   Calculate reflection in fragment shader. 
   
   REQUIRED when using reflect_correct() in vertex shader.
 */
void reflect_correct (void) {
    // Reflect view vector about the plane defined by the normal
    // at the fragment
    vec3 r = reflect(fs_in.view, normalize(fs_in.normal));

	// reverse view transformation of reflection to correctly project in cubemap
	r = inverse(fs_in.reverse_matrix) * r;
	
    // Sample from scaled using reflection vector
    color = texture(tex_cubemap, r);
}

/*
  Simply use the interpolated texture coordinates received
  from vertex shader.
  
  REQUIRED when using reflect_interpolate() in vertex shader.
 */ 
void reflect_interpolate(void) {
    color = texture(tex_cubemap, fs_in.view);
}


void main(void)
{
	// Two options. See vertex shader for more info.
	reflect_interpolate();
	//reflect_correct();
}
