
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

public final class DBeaverSplashHandler extends BasicSplashHandler {
    private Font normalFont;
    private Font boldFont;

    @Override
    public void init(Shell splash) {
        super.init(splash);

        try {
            initVisualization();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }
    
    private void initVisualization() {
        String progressRectString = null;
        String messageRectString = null;
        String foregroundColorString = null;
        String versionCoordString = null;
        String versionInfoSizeString = null;

        final IProduct product = Platform.getProduct();
        if (product != null) {
            progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
            messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
            foregroundColorString = product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
            versionCoordString = product.getProperty("versionInfoCoord");
            versionInfoSizeString = product.getProperty("versionInfoSize");
        }

        setProgressRect(StringConverter.asRectangle(progressRectString, new Rectangle(275, 300, 280, 10)));
        setMessageRect(StringConverter.asRectangle(messageRectString, new Rectangle(275, 275, 280, 25)));
        setForeground(StringConverter.asRGB(foregroundColorString, new RGB(255, 255, 255)));

        final Point versionCoord = StringConverter.asPoint(versionCoordString, new Point(485, 215));
        final int versionInfoSize = StringConverter.asInt(versionInfoSizeString, 22);

        normalFont = getContent().getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(versionInfoSize);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        getContent().addPaintListener(e -> {
            String productVersion = "";
            if (product != null) {
                productVersion = GeneralUtils.getPlainVersion();
            }
            e.gc.setFont(boldFont);
            e.gc.setForeground(getForeground());
            e.gc.drawText(productVersion, versionCoord.x, versionCoord.y, true);
            e.gc.setFont(normalFont);
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        if (boldFont != null) {
            boldFont.dispose();
            boldFont = null;
        }
    }
}
