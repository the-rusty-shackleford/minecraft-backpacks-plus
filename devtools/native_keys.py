# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
"""Deliver test keys through X11/XTest, without changing Minecraft key mappings."""
import ctypes as c

def key(name: str, down: bool) -> None:
    """Requires the focused muted test client; effects: sends one press or release."""
    assert name in ('g', 'b', 'Escape')
    x = c.CDLL('libX11.so.6')
    test = c.CDLL('libXtst.so.6')
    x.XOpenDisplay.argtypes = [c.c_char_p]
    x.XOpenDisplay.restype = c.c_void_p
    x.XStringToKeysym.argtypes = [c.c_char_p]
    x.XStringToKeysym.restype = c.c_ulong
    x.XKeysymToKeycode.argtypes = [c.c_void_p, c.c_ulong]
    x.XKeysymToKeycode.restype = c.c_uint
    x.XFlush.argtypes = [c.c_void_p]
    x.XCloseDisplay.argtypes = [c.c_void_p]
    test.XTestFakeKeyEvent.argtypes = [c.c_void_p, c.c_uint, c.c_int, c.c_ulong]
    display = x.XOpenDisplay(None)
    assert display, 'No host X11 display'
    try:
        code = x.XKeysymToKeycode(display, x.XStringToKeysym(name.encode()))
        assert code and test.XTestFakeKeyEvent(display, code, int(down), 0)
        x.XFlush(display)
    finally:
        x.XCloseDisplay(display)
