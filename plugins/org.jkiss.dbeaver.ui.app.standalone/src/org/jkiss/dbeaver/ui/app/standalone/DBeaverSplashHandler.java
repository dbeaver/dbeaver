
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
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public final class DBeaverSplashHandler extends BasicSplashHandler {
    private Font normalFont;
    private Font boldFont;
    private Label messageLabel;
    private final Log.Listener debugListener = new Log.Listener() {
        @Override
        public void loggedMessage(@Nullable Object message, @Nullable Throwable exception) {
            loggedMessage(message, exception, true);
        }

        @Override
        public void loggedMessage(@Nullable Object message, @Nullable Throwable exception, boolean notifyUI) {
            Shell splash = getSplash();
            if (notifyUI && message != null && splash != null && !splash.isDisposed()) {
                String text = message.toString();
                if (Thread.currentThread() == splash.getDisplay().getThread()) {
                    showMessage(text);
                } else {
                    splash.getDisplay().asyncExec(() -> showMessage(text));
                }
            }
        }
    };

    private void showMessage(@NotNull String text) {
        if (messageLabel != null && !messageLabel.isDisposed()) {
            messageLabel.setText(text.replace("&", "&&"));
            messageLabel.update();
        }
    }

    @Override
    public void init(@NotNull Shell splash) {
        super.init(splash);

        try {
            initVisualization();
            Log.addListener(debugListener);
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
        setForeground(StringConverter.asRGB(foregroundColorString, new RGB(255, 255, 255)));
        // The progress monitor throttles label updates, which can leave startup debug messages invisible.
        // Keep its progress indicator and render messages directly on the monitor's content instead.
        messageLabel = new Label(getContent(), SWT.NONE);
        messageLabel.setBounds(StringConverter.asRectangle(messageRectString, new Rectangle(275, 275, 280, 25)));
        messageLabel.setForeground(getForeground());

        final Point versionCoord = StringConverter.asPoint(versionCoordString, new Point(485, 215));
        final int versionInfoSize = StringConverter.asInt(versionInfoSizeString, 22);

        normalFont = getContent().getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(versionInfoSize);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        getContent().addPaintListener(e -> {
            var image = getSplash().getBackgroundImage();
            if (image != null && !isImageFlipped()) {
                // On macOS, since Sonoma, the splash is flipped. Eclipse features a fix for 14.x,
                // but the bug is present in later versions as well. Interestingly, drawImage draws
                // the image correctly; we just need to skip this for 14.x as it would flip it back.
                // See https://github.com/eclipse-platform/eclipse.platform.ui/blob/d0eb817372ead45fff370ae91560344e88b793b6/bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/internal/Workbench.java#L910-L925
                e.gc.drawImage(image, 0, 0);
            }
            e.gc.setFont(boldFont);
            e.gc.setForeground(getForeground());
            e.gc.drawText(GeneralUtils.getPlainVersion(), versionCoord.x, versionCoord.y, true);
            e.gc.setFont(normalFont);
        });
    }

    private static boolean isImageFlipped() {
        // See org.eclipse.ui.internal.Workbench.getImage
        return Util.isMac() && RuntimeUtils.getOSVersion().getMajor() == 14;
    }

    @Override
    public void dispose() {
        Log.removeListener(debugListener);
        messageLabel = null;
        Shell splash = getSplash();
        if (splash != null && !splash.isDisposed()) {
            super.dispose();
        }
        if (boldFont != null) {
            boldFont.dispose();
            boldFont = null;
        }
    }
}
