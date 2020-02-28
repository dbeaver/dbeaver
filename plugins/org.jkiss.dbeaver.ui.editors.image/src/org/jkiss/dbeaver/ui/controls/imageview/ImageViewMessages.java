/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.osgi.util.NLS;

public class ImageViewMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.controls.imageview.ImageViewMessages"; //$NON-NLS-1$

	public static String controls_imageview_fit_window;
	public static String controls_imageview_original_size;
	public static String controls_imageview_rotate;
	public static String controls_imageview_zoom_in;
	public static String controls_imageview_zoom_out;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ImageViewMessages.class);
	}

	private ImageViewMessages() {
	}
}
