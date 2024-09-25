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
package org.jkiss.dbeaver.ui.win32;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.sun.jna.platform.win32.Wtsapi32;

import java.util.ArrayList;
import java.util.List;

public class Win32SystemMonitor implements WindowProc {

    public static final String WINDOW_CLASS = "DBeaverWindowClass";
    /**
     * Powner events
     */

    /*
     * Notifies applications that a power-management event has occurred.
     */
    static final int WM_POWERBROADCAST = 536;

    /*
     * Power status has changed.
     */
    static final int PBT_APMPOWERSTATUSCHANGE = 10;
    static final int PBT_APMRESUMESUSPEND = 7;
    static final int PBT_APMSUSPEND = 4;
    static final int PBT_POWERSETTINGCHANGE = 32787;

    private final List<IWindowsSystemMonitorListener> listeners = new ArrayList<>();

    private HWND hWnd;
    private HMODULE hInst;

    public Win32SystemMonitor() {

    }

    public void removeListener(IWindowsSystemMonitorListener listener) {
        listeners.remove(listener);
    }

    public void addListener(IWindowsSystemMonitorListener listener) {
        listeners.add(listener);
    }

    public void startEventsListener() {
        hInst = Kernel32.INSTANCE.GetModuleHandle("");

        WNDCLASSEX wClass = new WNDCLASSEX();
        wClass.hInstance = hInst;
        wClass.lpfnWndProc = Win32SystemMonitor.this;
        wClass.lpszClassName = WINDOW_CLASS;

        // register window class
        User32.INSTANCE.RegisterClassEx(wClass);

        // create new window
        hWnd = User32.INSTANCE
            .CreateWindowEx(
                User32.WS_EX_TOPMOST,
                WINDOW_CLASS,
                "Event monitor",
                0, 0, 0, 0, 0,
                null, // WM_DEVICECHANGE contradicts parent=WinUser.HWND_MESSAGE
                null, hInst, null);

        Wtsapi32.INSTANCE.WTSRegisterSessionNotification(hWnd,
            Wtsapi32.NOTIFY_FOR_THIS_SESSION);
    }

    public void stopEventsListening() {
        Wtsapi32.INSTANCE.WTSUnRegisterSessionNotification(hWnd);
        User32.INSTANCE.UnregisterClass(WINDOW_CLASS, hInst);
        User32.INSTANCE.DestroyWindow(hWnd);
    }

    /*
     * uMSG 689 => session change
     * uMSG 536 => power change
     */

    @Override
    public LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
        switch (uMsg) {
            case WM_POWERBROADCAST: {
                this.onPowerChange(wParam, lParam);
                return new LRESULT(0);
            }
            case WinUser.WM_SESSION_CHANGE: {
                this.onSessionChange(wParam, lParam);
                return new LRESULT(0);
            }
            default:
                return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
        }
    }

    protected void onSessionChange(WPARAM wParam, LPARAM lParam) {
        switch (wParam.intValue()) {
            case Wtsapi32.WTS_SESSION_LOGON: {
                for (IWindowsSystemMonitorListener l : listeners) {
                    l.handleDesktopLogon();
                }
                break;
            }
            case Wtsapi32.WTS_SESSION_LOGOFF: {
                for (IWindowsSystemMonitorListener l : listeners) {
                    l.handleDesktopLogoff();
                }
                break;
            }
            case Wtsapi32.WTS_SESSION_LOCK: {
                for (IWindowsSystemMonitorListener l : listeners) {
                    l.handleDesktopLocked();
                }

                break;
            }
            case Wtsapi32.WTS_SESSION_UNLOCK: {
                for (IWindowsSystemMonitorListener l : listeners) {
                    l.handleDesktopUnlocked();
                }
                break;
            }
        }
    }

    protected void onPowerChange(WPARAM wParam, LPARAM lParam) {
        switch (wParam.intValue()) {
            case PBT_APMSUSPEND: {
                for (IWindowsSystemMonitorListener l : listeners) {
                    l.onMachineGoingToSleep();
                }
                break;
            }
            case PBT_APMRESUMESUSPEND: {
                for (IWindowsSystemMonitorListener l : listeners) {
                    l.onMachineGoingToAwake();
                }
                break;
            }
        }
    }

}