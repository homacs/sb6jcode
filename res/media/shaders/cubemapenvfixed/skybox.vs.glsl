#version 410 core

out VS_OUT
{
    vec3    tc;
} vs_out;

uniform mat4 view_matrix;



void main(void)
{
	/*
	 The skybox is projected using a rectangle which exactly fills the entire
	 screen: (-1,+1) is the upper left corner and (+1,-1) the lower right corner.
	 Since the rectangle is moved 1 meter aways from the world center, all the 
	 vectors to vertices point from the center away towards -z. These are the 
	 view directions we would like to project onto the inner side of the cube
	 map to fetch the corresponding texel.
	 
	 Camera rotation is given by the view matrix. The view matrix actually rotates
	 and moves the world with all objects in the opposite direction of the movement
     of the camera to make it look like the camera where moved. In view space the 
     camera is in the center pointing towards -z and all the objects are moved 
     accordingly to simulate the camera movement.
     
     The cubemap is steady. It always is aligned with the camera (e.g. in view space). 
     To make it look like the camera was moved or is rotating, we have to rotate the 
     view directions of the skybox according with the camera movement. Since the view
	 matrix actually rotates objects against the camera movement, we use the inverse 
	 view matrix, to create a matrix which does just the opposite.
      
     Translational movement of distance objects gets slower and slower the further away
     those objects are. You can litterally observe it, when you look out of window of 
     a driving car or train. Distant objects move much slower then objects very 
     close to you. Since we assume the cube to be of infinite size (texels on the cube's 
     surface are incredibly far away), the translational movement of texels on the cube 
     is zero. Therefore we use just the rotational part of the view matrix which is the 
     upper left 3x3 matrix of the usual 4x4 matrix we have got.
     
     This skybox obviously does not work for skyboxes, which have a floor at foot level.
     In this case you need an actual box placed in world coordinates which is textured from
     the inside and gets rotated and translated in view space like all other objects.
	*/
    vec3[4] vertices = vec3[4](vec3(-1.0, -1.0, -1.0),
                               vec3( 1.0, -1.0, -1.0),
                               vec3(-1.0,  1.0, -1.0),
                               vec3( 1.0,  1.0, -1.0));

    vs_out.tc = inverse(mat3(view_matrix)) * vertices[gl_VertexID];

    gl_Position = vec4(vertices[gl_VertexID], 1.0);
}
