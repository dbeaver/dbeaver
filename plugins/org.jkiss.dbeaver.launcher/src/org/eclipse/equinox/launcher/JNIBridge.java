/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc - Default the configuration to Application Support (bug 461725)
 *******************************************************************************/
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
	 * @noreference This constructor is not intended to be referenced by clients.
	 *
	 * @param library the given library
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
	boolean isLibraryLoadedByJava() {
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
