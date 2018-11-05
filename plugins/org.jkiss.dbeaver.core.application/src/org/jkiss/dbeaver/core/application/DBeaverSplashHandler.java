
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * @since 3.3
 * 
 */
public class DBeaverSplashHandler extends BasicSplashHandler {

	public static final int TOTAL_LOADING_TASKS = 20;
	private static DBeaverSplashHandler instance;

    public static IProgressMonitor getActiveMonitor()
    {
        if (instance == null) {
            return null;
        } else {
            try {
                return instance.getBundleProgressMonitor();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return null;
            }
        }
    }

    private Font normalFont;
    private Font boldFont;

    public DBeaverSplashHandler()
    {
        instance = this;
    }

    @Override
    public void init(Shell splash) {
        super.init(splash);

        try {
            initVisualization();

            getBundleProgressMonitor().beginTask("Loading", TOTAL_LOADING_TASKS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

    private void initVisualization() {
        String progressRectString = null, messageRectString = null, foregroundColorString = null,
            versionCoordString = null, versionInfoSizeString = null, versionInfoColorString = null;
        final IProduct product = Platform.getProduct();
        if (product != null) {
            progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
            messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
            foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
            versionCoordString = product.getProperty("versionInfoCoord");
            versionInfoSizeString = product.getProperty("versionInfoSize");
            versionInfoColorString = product.getProperty("versionInfoColor");
        }

        setProgressRect(StringConverter.asRectangle(progressRectString, new Rectangle(275, 300, 280, 10)));
        setMessageRect(StringConverter.asRectangle(messageRectString, new Rectangle(275,275,280,25)));
        final Point versionCoord = StringConverter.asPoint(versionCoordString, new Point(485, 215));
        final int versionInfoSize = StringConverter.asInt(versionInfoSizeString, 22);
        final RGB versionInfoRGB = StringConverter.asRGB(versionInfoColorString, new RGB(255,255,255));

        int foregroundColorInteger = 0xD2D7FF;
        try {
			if (foregroundColorString != null) {
				foregroundColorInteger = Integer.parseInt(foregroundColorString, 16);
			}
        } catch (Exception ex) {
            // ignore
        }

        setForeground(
			new RGB(
				(foregroundColorInteger & 0xFF0000) >> 16,
                (foregroundColorInteger & 0xFF00) >> 8,
                foregroundColorInteger & 0xFF));

        normalFont = getContent().getFont();
        //boldFont = UIUtils.makeBoldFont(normalFont);
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(versionInfoSize);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        final Color versionColor = new Color(getContent().getDisplay(), versionInfoRGB);

        getContent().addPaintListener(e -> {
            String productVersion = "";
            if (product != null) {
                productVersion = GeneralUtils.getPlainVersion();
            }
            //String osVersion = Platform.getOS() + " " + Platform.getOSArch();
            if (boldFont != null) {
                e.gc.setFont(boldFont);
            }
            e.gc.setForeground(versionColor);
            e.gc.drawText(productVersion, versionCoord.x, versionCoord.y, true);
            //e.gc.drawText(osVersion, 115, 200, true);
            e.gc.setFont(normalFont);
        });
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (boldFont != null) {
            boldFont.dispose();
            boldFont = null;
        }
        instance = null;
    }

	public static void showMessage(String message) {
		if (message == null || message.isEmpty() || message.startsWith(">")) {
			return;
		}
        try {
            IProgressMonitor activeMonitor = getActiveMonitor();
            if (activeMonitor != null) {
                activeMonitor.setTaskName(message);
                activeMonitor.worked(1);
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

}
