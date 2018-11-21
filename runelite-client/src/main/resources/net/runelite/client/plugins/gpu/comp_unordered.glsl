/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#version 430 core

#define PI 3.1415926535897932384626433832795f
#define UNIT PI / 1024.0f

layout(std140) uniform uniforms {
  int cameraYaw;
  int cameraPitch;
  int centerX;
  int centerY;
  int zoom;
  ivec2 sinCosTable[2048];
};

struct modelinfo {
  int offset;   // offset into buffer
  int uvOffset; // offset into uv buffer
  int length;   // length in faces
  int idx;      // write idx in target buffer
  int flags;    // radius, orientation
  int x;        // scene position x
  int y;        // scene position y
  int z;        // scene position z
};

layout(std430, binding = 0) readonly buffer modelbuffer_in {
  modelinfo ol[];
};

layout(std430, binding = 1) readonly buffer vertexbuffer_in {
  ivec4 vb[];
};

layout(std430, binding = 2) readonly buffer tempvertexbuffer_in {
  ivec4 tempvb[];
};

layout(std430, binding = 3) writeonly buffer vertex_out {
  ivec4 vout[];
};

layout(std430, binding = 4) writeonly buffer uv_out {
  vec4 uvout[];
};

layout(std430, binding = 5) readonly buffer uvbuffer_in {
  vec4 uv[];
};

layout(std430, binding = 6) readonly buffer tempuvbuffer_in {
  vec4 tempuv[];
};

layout(local_size_x = 6) in;

/*
 * Rotate a vertex by a given orientation in JAU
 */
ivec4 rotate(ivec4 vertex, int orientation) {
  ivec2 sinCos = sinCosTable[orientation];
  int s = sinCos.x;
  int c = sinCos.y;
  int x = vertex.z * s + vertex.x * c >> 16;
  int z = vertex.z * c - vertex.x * s >> 16;
  return ivec4(x, vertex.y, z, vertex.w);
}

void main() {
  uint groupId = gl_WorkGroupID.x;
  uint localId = gl_LocalInvocationID.x;
  modelinfo minfo = ol[groupId];

  int offset = minfo.offset;
  int length = minfo.length;
  int outOffset = minfo.idx;
  int uvOffset = minfo.uvOffset;
  int flags = minfo.flags;
  int orientation = flags & 0x7ff;
  ivec4 pos = ivec4(minfo.x, minfo.y, minfo.z, 0);

  if (localId >= length) {
    return;
  }

  uint ssboOffset = localId;
  ivec4 thisA, thisB, thisC;

  // Grab triangle vertices from the correct buffer
  if (flags < 0) {
    thisA = vb[offset + ssboOffset * 3    ];
    thisB = vb[offset + ssboOffset * 3 + 1];
    thisC = vb[offset + ssboOffset * 3 + 2];
  } else {
    thisA = tempvb[offset + ssboOffset * 3    ];
    thisB = tempvb[offset + ssboOffset * 3 + 1];
    thisC = tempvb[offset + ssboOffset * 3 + 2];
  }

  ivec4 thisrvA = rotate(thisA, orientation);
  ivec4 thisrvB = rotate(thisB, orientation);
  ivec4 thisrvC = rotate(thisC, orientation);

  uint myOffset = localId;

  // position vertices in scene and write to out buffer
  vout[outOffset + myOffset * 3]     = pos + thisrvA;
  vout[outOffset + myOffset * 3 + 1] = pos + thisrvB;
  vout[outOffset + myOffset * 3 + 2] = pos + thisrvC;

  if (uvOffset < 0) {
    uvout[outOffset + myOffset * 3]     = vec4(0, 0, 0, 0);
    uvout[outOffset + myOffset * 3 + 1] = vec4(0, 0, 0, 0);
    uvout[outOffset + myOffset * 3 + 2] = vec4(0, 0, 0, 0);
  } else if (flags >= 0) {
    uvout[outOffset + myOffset * 3]     = tempuv[uvOffset + localId * 3];
    uvout[outOffset + myOffset * 3 + 1] = tempuv[uvOffset + localId * 3 + 1];
    uvout[outOffset + myOffset * 3 + 2] = tempuv[uvOffset + localId * 3 + 2];
  } else {
    uvout[outOffset + myOffset * 3]     = uv[uvOffset + localId * 3];
    uvout[outOffset + myOffset * 3 + 1] = uv[uvOffset + localId * 3 + 1];
    uvout[outOffset + myOffset * 3 + 2] = uv[uvOffset + localId * 3 + 2];
  }
}
