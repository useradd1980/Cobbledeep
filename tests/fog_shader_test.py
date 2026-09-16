"""Compile and render the actual GLSL with a headless EGL OpenGL context.

Requires Mesa EGL/OpenGL libraries; no Python packages. Does not test Forge hooks.
"""
import ctypes as C
import json
from pathlib import Path

E = C.CDLL("libEGL.so.1")
G = C.CDLL("libGL.so.1")
I, U, F, P = C.c_int, C.c_uint, C.c_float, C.c_void_p

def fn(lib, name, result, *args):
    f = getattr(lib, name)
    f.restype, f.argtypes = result, args
    return f

get_proc = fn(E, "eglGetProcAddress", P, C.c_char_p)
platform = C.CFUNCTYPE(P, U, P, C.POINTER(I))(get_proc(b"eglGetPlatformDisplayEXT"))
display = platform(0x31DD, None, None)  # EGL_PLATFORM_SURFACELESS_MESA
major, minor = I(), I()
assert fn(E, "eglInitialize", U, P, C.POINTER(I), C.POINTER(I))(display, C.byref(major), C.byref(minor))
assert fn(E, "eglBindAPI", U, U)(0x30A2)  # OpenGL
attrs = (I * 13)(0x3033, 1, 0x3040, 8, 0x3024, 8, 0x3023, 8, 0x3022, 8, 0x3021, 8, 0x3038)
config, count = P(), I()
assert fn(E, "eglChooseConfig", U, P, C.POINTER(I), C.POINTER(P), I, C.POINTER(I))(
    display, attrs, C.byref(config), 1, C.byref(count)) and count.value
pattrs = (I * 5)(0x3057, 4, 0x3056, 4, 0x3038)
surface = fn(E, "eglCreatePbufferSurface", P, P, P, C.POINTER(I))(display, config, pattrs)
cattrs = (I * 7)(0x3098, 3, 0x30FB, 2, 0x30FD, 1, 0x3038)
context = fn(E, "eglCreateContext", P, P, P, P, C.POINTER(I))(display, config, None, cattrs)
assert context and surface
assert fn(E, "eglMakeCurrent", U, P, P, P, P)(display, surface, surface, context)

root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/cobbledeep/shaders/core"
create = fn(G, "glCreateShader", U, U)
source = fn(G, "glShaderSource", None, U, I, C.POINTER(C.c_char_p), P)
compile_shader = fn(G, "glCompileShader", None, U)
get_shader = fn(G, "glGetShaderiv", None, U, U, C.POINTER(I))
shader_log = fn(G, "glGetShaderInfoLog", None, U, I, P, P)
program = fn(G, "glCreateProgram", U)()
for extension, kind in [("vsh", 0x8B31), ("fsh", 0x8B30)]:
    shader = create(kind)
    data = C.c_char_p((root / ("tactical_fog." + extension)).read_bytes())
    source(shader, 1, C.byref(data), None)
    compile_shader(shader)
    ok = I()
    get_shader(shader, 0x8B81, C.byref(ok))
    log = C.create_string_buffer(8192)
    shader_log(shader, len(log), None, log)
    assert ok.value, log.value.decode()
    fn(G, "glAttachShader", None, U, U)(program, shader)
fn(G, "glLinkProgram", None, U)(program)
ok = I()
fn(G, "glGetProgramiv", None, U, U, C.POINTER(I))(program, 0x8B82, C.byref(ok))
assert ok.value, "Shader link failed"
fn(G, "glUseProgram", None, U)(program)

uniform = fn(G, "glGetUniformLocation", I, U, C.c_char_p)
for entry in json.loads((root / "tactical_fog.json").read_text())["uniforms"]:
    assert uniform(program, entry["name"].encode()) >= 0, entry["name"]
matrix = fn(G, "glUniformMatrix4fv", None, I, I, U, C.POINTER(F))
set3 = fn(G, "glUniform3f", None, I, F, F, F)
ident = (F * 16)(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1)
matrix(uniform(program, b"InverseProjection"), 1, 0, ident)
matrix(uniform(program, b"InverseView"), 1, 0, ident)
set3(uniform(program, b"CameraPosition"), 0, 0, 0)
set3(uniform(program, b"GridOrigin"), -64, -64, -64)

vao, vbo = U(), U()
fn(G, "glGenVertexArrays", None, I, C.POINTER(U))(1, C.byref(vao))
fn(G, "glBindVertexArray", None, U)(vao)
fn(G, "glGenBuffers", None, I, C.POINTER(U))(1, C.byref(vbo))
fn(G, "glBindBuffer", None, U, U)(0x8892, vbo)
verts = (F * 20)(-1,-1,0,0,0, 1,-1,0,1,0, 1,1,0,1,1, -1,1,0,0,1)
fn(G, "glBufferData", None, U, C.c_ssize_t, P, U)(0x8892, C.sizeof(verts), verts, 0x88E4)
for name, size, offset in [(b"Position", 3, 0), (b"UV0", 2, 12)]:
    loc = fn(G, "glGetAttribLocation", I, U, C.c_char_p)(program, name)
    fn(G, "glEnableVertexAttribArray", None, U)(loc)
    fn(G, "glVertexAttribPointer", None, U, I, U, U, I, P)(loc, size, 0x1406, 0, 20, P(offset))

active = fn(G, "glActiveTexture", None, U)
bind = fn(G, "glBindTexture", None, U, U)
upload = fn(G, "glTexImage2D", None, U, I, I, I, I, I, U, U, P)
textures = (U * 2)()
fn(G, "glGenTextures", None, I, C.POINTER(U))(2, textures)
for unit, name in enumerate([b"DepthSampler", b"FogSampler"]):
    active(0x84C0 + unit)
    bind(0x0DE1, textures[unit])
    for pname in (0x2801, 0x2800):
        fn(G, "glTexParameteri", None, U, U, I)(0x0DE1, pname, 0x2600)
    fn(G, "glUniform1i", None, I, I)(uniform(program, name), unit)

# Mirror Minecraft's offscreen colour/depth target, and exercise the real depth
# copy operation rather than feeding a stand-in colour texture to the shader.
framebuffer, colour, depth_buffer = U(), U(), U()
fn(G, "glGenFramebuffers", None, I, C.POINTER(U))(1, C.byref(framebuffer))
fn(G, "glBindFramebuffer", None, U, U)(0x8D40, framebuffer)
fn(G, "glGenTextures", None, I, C.POINTER(U))(1, C.byref(colour))
bind(0x0DE1, colour)
upload(0x0DE1, 0, 0x8058, 4, 4, 0, 0x1908, 0x1401, None)
fn(G, "glFramebufferTexture2D", None, U, U, U, U, I)(0x8D40, 0x8CE0, 0x0DE1, colour, 0)
fn(G, "glGenRenderbuffers", None, I, C.POINTER(U))(1, C.byref(depth_buffer))
fn(G, "glBindRenderbuffer", None, U, U)(0x8D41, depth_buffer)
fn(G, "glRenderbufferStorage", None, U, U, I, I)(0x8D41, 0x81A6, 4, 4)
fn(G, "glFramebufferRenderbuffer", None, U, U, U, U)(0x8D40, 0x8D00, 0x8D41, depth_buffer)
assert fn(G, "glCheckFramebufferStatus", U, U)(0x8D40) == 0x8CD5

def depth(value):
    active(0x84C0)
    bind(0x0DE1, textures[0])
    fn(G, "glClearDepth", None, C.c_double)(value)
    fn(G, "glClear", None, U)(0x0100)
    fn(G, "glCopyTexImage2D", None, U, I, U, I, I, I, I, I)(0x0DE1, 0, 0x81A6, 0, 0, 4, 4, 0)

def fog(data):
    active(0x84C1)
    bind(0x0DE1, textures[1])
    values = (C.c_ubyte * len(data)).from_buffer_copy(data)
    upload(0x0DE1, 0, 0x8229, 2048, 1024, 0, 0x1903, 0x1401, values)

fn(G, "glViewport", None, I, I, I, I)(0, 0, 4, 4)
fn(G, "glDisable", None, U)(0x0B71)
fn(G, "glEnable", None, U)(0x0BE2)
fn(G, "glBlendFunc", None, U, U)(0x0302, 0x0303)

checks = 0
def draw(expected):
    global checks
    fn(G, "glClearColor", None, F, F, F, F)(0.8, 0.6, 0.4, 1)
    fn(G, "glClear", None, U)(0x4000)
    fn(G, "glDrawArrays", None, U, I, I)(0x0006, 0, 4)
    pixel = (C.c_ubyte * 4)()
    fn(G, "glReadPixels", None, I, I, I, I, U, U, P)(2, 2, 1, 1, 0x1908, 0x1401, pixel)
    assert all(abs(pixel[i] / 255 - expected[i]) < 0.025 for i in range(3)), (list(pixel), expected)
    assert fn(G, "glGetError", U)() == 0, "OpenGL error"
    checks += 1

depth(0.5)
for state, expected in [(0, (.035,.045,.06)), (1, (.32,.24,.16)), (2, (.32,.24,.16))]:
    fog(bytes([state]) * (128**3))
    draw(expected)
depth(1)
draw((.035,.045,.06))
depth(0.5)
set3(uniform(program, b"GridOrigin"), 1000, 1000, 1000)
draw((.035,.045,.06))

# Non-identity matrices plus translated camera: only the correctly reconstructed
# voxel is visible. This detects transposed matrices / wrong world-space order.
matrix(uniform(program, b"InverseProjection"), 1, 0,
       (F * 16)(2,0,0,0, 0,3,0,0, 0,0,4,0, 0,0,0,1))
matrix(uniform(program, b"InverseView"), 1, 0,
       (F * 16)(0,0,-1,0, 0,1,0,0, 1,0,0,0, 0,0,0,1))
set3(uniform(program, b"CameraPosition"), 10, 20, 30)
set3(uniform(program, b"GridOrigin"), 0, 0, 0)
set1 = fn(G, "glUniform1f", None, I, F)
set1(uniform(program, b"TerrainRange"), 24)
set3(uniform(program, b"PlayerPosition"), 10, 20, 30)
pixels = bytearray(128**3)
pixels[5 + 128 * (14 + 128 * 10)] = 2
fog(pixels)
draw((.8,.6,.4))
# Nearby terrain is never black, even before memory arrives or at other heights.
fog(bytes(128**3))
for height in [-200, 20, 300]:
    set3(uniform(program, b"PlayerPosition"), 10, height, 30)
    draw((.32,.24,.16))
set3(uniform(program, b"PlayerPosition"), 33.5, 20, 29.5)
draw((.32,.24,.16))
set3(uniform(program, b"PlayerPosition"), 34.5, 20, 29.5)
draw((.035,.045,.06))
# Move the character away without moving the camera: unknown returns opaque,
# remembered ground dims. Camera position alone never grants visibility.
set3(uniform(program, b"PlayerPosition"), 100, 20, 100)
draw((.035,.045,.06))
fog(bytes([1]) * (128**3))
draw((.32,.24,.16))
# A room in range dims when blocked, clears with sight through the doorway,
# and dims again when sight is lost. Neither case can erase its discovery.
set3(uniform(program, b"PlayerPosition"), 10, 20, 30)
for state, expected in [(1, (.32,.24,.16)), (2, (.8,.6,.4)), (1, (.32,.24,.16))]:
    fog(bytes([state]) * (128**3))
    draw(expected)
# Missing atlas coverage is also dim (not black) inside the radius.
set3(uniform(program, b"GridOrigin"), 1000, 1000, 1000)
draw((.32,.24,.16))
print(f"GLSL compile/link, resource uniforms, depth copying and {checks} framebuffer checks passed.")
