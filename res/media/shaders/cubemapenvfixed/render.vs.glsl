#version 410 core

uniform mat4 mv_matrix;
uniform mat4 reverse_matrix; // TODO: input inverse mv matrix from application
uniform mat4 proj_matrix;

layout (location = 0) in vec4 position;
layout (location = 1) in vec3 normal;

out VS_OUT
{
    vec3 normal;
    vec3 view;
    flat mat3 reverse_matrix;
} vs_out;


void reflect_correct(void) {
    vec4 pos_vs = mv_matrix * position;

    vs_out.normal = (mat3(mv_matrix) * normal);
    vs_out.view = (pos_vs.xyz);

	// provide a matrix to reverse the view transformation
	// to redirect the reflection according to the camera angle.    
    vs_out.reverse_matrix = mat3(reverse_matrix);

    gl_Position = proj_matrix * pos_vs;
}

//
// This function calulates the reflection vector for each vertex and
// lets open gl interpolate it over the plane. The fragment shader
// needs no more calculation in this case and can just fetch the texel
// with the given texture coordinates.
//
void reflect_interpolate(void) {
    vec4 pos_vs = mv_matrix * position;

    vec3 N = (mat3(mv_matrix) * normal);
    
    vec3 r = reflect(pos_vs.xyz, N);

	// reverse view transformation
	vs_out.view = mat3(reverse_matrix) * r;

    gl_Position = proj_matrix * pos_vs;
}


void main(void)
{
	/* 
	   As explained in the skybox shader, the cubemap is steady and always 
	   aligned with the camera in view space. Objects on the other side, 
	   are moved in the opposite direction of the camera movement to find
	   the correct places relative to the camera which is located in the 
	   center of the view space, pointing towards -z. Thus, if we rotate 
	   the object with the view matrix into view space the calculated 
	   reflection vector will point at a location in the cubemap which is 
	   not rotated according to the camera (since the cubemap is steady). 
	   Since the cubemap is aligned with the camera (i.e. always rotates with 
	   the camera), we need to rotate the calculated reflection vector 
	   accordingly, which means, with the camera. Since the view matrix
	   rotates in the opposite direction of the camera, we use the inverse view
	   matrix to rotate the reflection vector with the camera. Now, since both
	   reflection vector and cubemap are rotated with the camera, we will fetch 
	   the accurate texel for the reflection.
	 */
	 
	 
	 /*
	   Here we have two methods to achieve the above. 
	   1. reflect_correct(): Calculate normal and view in vertex shader and 
	                         calculate reflection in fragement shader.
	   2. reflect_interpolated(): Calculate reflection in vertex shader and
	                         let OpenGL interpolate it over the plane.
	   You can chose either of them but you have to adjust the fragment shader
	   as well.
	 */
	reflect_interpolate();
	// reflect_correct();
}
