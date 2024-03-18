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
package org.eclipse.equinox.launcher;

/**
 * <b>Note:</b> This class should not be referenced programmatically by
 * other Java code. This class exists only for the purpose of interacting with
 * a native launcher. To launch Eclipse programmatically, use
 * org.eclipse.core.runtime.adaptor.EclipseStarter. This class is not API.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class JNIBridge {
    //TODO: This class should not be public
    private native void _set_exit_data(String sharedId, String data);

    private native void _set_launcher_info(String launcher, String name);

    private native void _update_splash();

    private native long _get_splash_handle();

    private native void _show_splash(String bitmap);

    private native void _takedown_splash();

    private native String _get_os_recommended_folder();

    private native int OleInitialize(int reserved);

    private native void OleUninitialize();

    private String library;
    private boolean libraryLoaded = false;

    /**
     * @param library the given library
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public JNIBridge(String library) {
        this.library = library;
    }

    private void loadLibrary() {
        if (library != null) {
            try {
                if (library.contains("wpf")) { //$NON-NLS-1$
                    int idx = library.indexOf("eclipse_"); //$NON-NLS-1$
                    if (idx != -1) {
                        String comLibrary = library.substring(0, idx) + "com_"; //$NON-NLS-1$
                        comLibrary += library.substring(idx + 8, library.length());
                        Runtime.getRuntime().load(comLibrary);
                        OleInitialize(0);
                    }
                }
                Runtime.getRuntime().load(library);
            } catch (UnsatisfiedLinkError e) {
                //failed
            }
        }
        libraryLoaded = true;
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean setExitData(String sharedId, String data) {
        try {
            _set_exit_data(sharedId, data);
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return setExitData(sharedId, data);
            }
            return false;
        }
    }

    /**
     * @noreference This method is not intended to be referenced by clients
     */
    public boolean setLauncherInfo(String launcher, String name) {
        try {
            _set_launcher_info(launcher, name);
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return setLauncherInfo(launcher, name);
            }
            return false;
        }
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean showSplash(String bitmap) {
        try {
            _show_splash(bitmap);
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return showSplash(bitmap);
            }
            return false;
        }
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean updateSplash() {
        try {
            _update_splash();
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return updateSplash();
            }
            return false;
        }
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public long getSplashHandle() {
        try {
            return _get_splash_handle();
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return getSplashHandle();
            }
            return -1;
        }
    }

    /**
     * Whether or not we loaded the shared library here from java.
     * False does not imply the library is not available, it could have
     * been loaded natively by the executable.
     *
     * @return boolean
     */
    public boolean isLibraryLoadedByJava() {
        return libraryLoaded;
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean takeDownSplash() {
        try {
            _takedown_splash();
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return takeDownSplash();
            }
            return false;
        }
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    public boolean uninitialize() {
        if (libraryLoaded && library != null) {
            if (library.contains("wpf")) { //$NON-NLS-1$
                try {
                    OleUninitialize();
                } catch (UnsatisfiedLinkError e) {
                    // library not loaded
                    return false;
                }
            }
        }
        return true;
    }

    public String getOSRecommendedFolder() {
        try {
            return _get_os_recommended_folder();
        } catch (UnsatisfiedLinkError e) {
            if (!libraryLoaded) {
                loadLibrary();
                return getOSRecommendedFolder();
            }
            return null;
        }
    }
}
