#version 420 core

layout (vertices = 4) out;

in VS_OUT
{
    vec2 tc;
} tcs_in[];

out TCS_OUT
{
    vec2 tc;
} tcs_out[];

uniform mat4 mvp_matrix;

//
// hm20150819: added experimental feature
//
// This factor allows to increase accuracy of displacement
// A low factor for example tends to cut the top of a hill 
// if the vertices lie around it while it renders the tip 
// if a vertex lies on top of it. You can see these effects 
// if you switch to wired mode (hit 'W').
// 
float general_factor = 16.0f * 64.0;


//
// hm20150819: added experimental feature
//
// This factor applies LOD decrease for polygones further 
// away from the viewer.
// Interval 0.0 = LOD off, 1.0 = full LOD application
// 
float lod_factor = 0.95;

void main(void)
{
    if (gl_InvocationID == 0)
    {
        vec4 p0 = mvp_matrix * gl_in[0].gl_Position;
        vec4 p1 = mvp_matrix * gl_in[1].gl_Position;
        vec4 p2 = mvp_matrix * gl_in[2].gl_Position;
        vec4 p3 = mvp_matrix * gl_in[3].gl_Position;
        p0 /= p0.w;
        p1 /= p1.w;
        p2 /= p2.w;
        p3 /= p3.w;
        
        //
        // hm20150819: added
        // I changed this to clip only if the face is entirely 
        // outside the viewport.
        //
        if (p0.z <= 0.0 &&
            p1.z <= 0.0 &&
            p2.z <= 0.0 &&
            p3.z <= 0.0)
         {
              gl_TessLevelOuter[0] = 0.0;
              gl_TessLevelOuter[1] = 0.0;
              gl_TessLevelOuter[2] = 0.0;
              gl_TessLevelOuter[3] = 0.0;
         }
         else
         {
            float l0 = length(p2.xy - p0.xy) * general_factor * (1.0 - lod_factor * (min(p2.z, p0.z))) + 1.0;
            float l1 = length(p3.xy - p2.xy) * general_factor * (1.0 - lod_factor * (min(p3.z, p2.z))) + 1.0;
            float l2 = length(p3.xy - p1.xy) * general_factor * (1.0 - lod_factor * (min(p3.z, p1.z))) + 1.0;
            float l3 = length(p1.xy - p0.xy) * general_factor * (1.0 - lod_factor * (min(p1.z, p0.z))) + 1.0;
            gl_TessLevelOuter[0] = l0;
            gl_TessLevelOuter[1] = l1;
            gl_TessLevelOuter[2] = l2;
            gl_TessLevelOuter[3] = l3;
            gl_TessLevelInner[0] = min(l1, l3);
            gl_TessLevelInner[1] = min(l0, l2);
        }
    }
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;
    tcs_out[gl_InvocationID].tc = tcs_in[gl_InvocationID].tc;
}
