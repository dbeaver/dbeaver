/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.gis.panel;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

class GISBrowserImageUtils {
    
    static ImageData getControlScreenshotOnWindows(Control control) {
        HWND sourceHwnd = new WinDef.HWND(Pointer.NULL);
        Point location = control.toDisplay(0, 0);
        Point size = control.getSize();
        
        HDC sourceHdc = User32.INSTANCE.GetDC(sourceHwnd);
        if (sourceHdc == null) {
            throw new Win32Exception(Native.getLastError());
        }

        ImageData data = null;
        Win32Exception error = null;
        HDC targetHdc = null;
        HBITMAP platformBitmapHandle = null;
        HANDLE originalObjHandle = null;
        
        try {
            targetHdc = GDI32.INSTANCE.CreateCompatibleDC(sourceHdc);
            if (targetHdc == null) {
                throw new Win32Exception(Native.getLastError());
            }

            platformBitmapHandle = GDI32.INSTANCE.CreateCompatibleBitmap(sourceHdc, size.x, size.y);
            if (platformBitmapHandle == null) {
                throw new Win32Exception(Native.getLastError());
            }

            originalObjHandle = GDI32.INSTANCE.SelectObject(targetHdc, platformBitmapHandle);
            if (originalObjHandle == null) {
                throw new Win32Exception(Native.getLastError());
            }

            if (!GDI32.INSTANCE.BitBlt(targetHdc, 0, 0, size.x, size.y, sourceHdc, location.x, location.y, GDI32.SRCCOPY)) {
                throw new Win32Exception(Native.getLastError());
            }

            BITMAPINFO bmi = new BITMAPINFO();
            bmi.bmiHeader.biWidth = size.x;
            bmi.bmiHeader.biHeight = -size.y;
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = 32;
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

            int bufferSize = size.x * size.y * 4;
            Memory buffer = new Memory(bufferSize);
            int resultOfDrawing = GDI32.INSTANCE.GetDIBits(sourceHdc, platformBitmapHandle, 0, size.y, buffer, bmi, WinGDI.DIB_RGB_COLORS);
            if (resultOfDrawing == 0 || resultOfDrawing == WinError.ERROR_INVALID_PARAMETER) {
                throw new Win32Exception(Native.getLastError());
            }

            byte[] rawData = buffer.getByteArray(0, bufferSize);
            PaletteData palette = new PaletteData(0x0000FF00, 0x00FF0000, 0xFF000000);
            data = new ImageData(size.x, size.y, 32, palette, size.x, rawData);
        } catch (Win32Exception e) {
            error = e;
        } finally {
            if (originalObjHandle != null) {
                // per MSDN, set the display surface back when done drawing
                HANDLE result = GDI32.INSTANCE.SelectObject(targetHdc, originalObjHandle);
                // failure modes are null or equal to HGDI_ERROR
                if (result == null || WinGDI.HGDI_ERROR.equals(result)) {
                    Win32Exception ex = new Win32Exception(Native.getLastError());
                    if (error != null) {
                        ex.addSuppressed(error);
                    }
                    error = ex;
                }
            }

            if (platformBitmapHandle != null) {
                if (!GDI32.INSTANCE.DeleteObject(platformBitmapHandle)) {
                    Win32Exception ex = new Win32Exception(Native.getLastError());
                    if (error != null) {
                        ex.addSuppressed(error);
                    }
                    error = ex;
                }
            }

            if (targetHdc != null) {
                if (!GDI32.INSTANCE.DeleteDC(targetHdc)) {
                    Win32Exception ex = new Win32Exception(Native.getLastError());
                    if (error != null) {
                        ex.addSuppressed(error);
                    }
                    error = ex;
                }
            }

            if (sourceHdc != null) {
                if (0 == User32.INSTANCE.ReleaseDC(sourceHwnd, sourceHdc)) {
                    throw new IllegalStateException("Device context did not release properly.");
                }
            }
        }

        if (error != null) {
            throw error;
        }
        return data;
    }
}
