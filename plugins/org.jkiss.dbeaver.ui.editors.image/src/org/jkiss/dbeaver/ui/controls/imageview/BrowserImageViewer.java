/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Image viewer control
 */
public class BrowserImageViewer extends AbstractImageViewer {

    private Browser browser;

    private Path tempFile;
    private ImageData imageData;

    public BrowserImageViewer(Composite parent, int style) {
        super(parent, style);

        GridLayout gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);
        int i = 0;


        browser = new Browser(this, SWT.NONE);
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        browser.setJavascriptEnabled(false); // We don't need java script to open images


        // Add DND support


    }

    @Override
    public boolean loadImage(InputStream inputStream) {
        try {
            ImageInputStream stream = ImageIO.createImageInputStream(inputStream); // assuming imgData
            // is byte[] as in your question

            imageData = new ImageData(inputStream);
            tempFile = Files.createTempFile("image", getImageType(imageData.type));
            ImageLoader imageLoader = new ImageLoader();
            imageLoader.data = new ImageData[1];
            imageLoader.data[0] = imageData;
            try (OutputStream fos = new FileOutputStream(tempFile.toFile())) {
                imageLoader.save(fos, imageData.type);
            } catch (IOException e) {
                DBWorkbench.getPlatformUI().showError("Image save error", "Error saving as picture", e);
            }
            URL url = tempFile.toUri().toURL();
            browser.setUrl(url.toString());
        } catch (IOException exception) {
            DBWorkbench.getPlatformUI().showError("Can't create a temp file", exception.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean clearImage() {
        browser.setUrl(null);
        return true;
    }

    @Override
    public void dispose() {
        if (tempFile != null) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
                // ignore
            }
        }
        // Edge uses its own callbacks which are not synchronized in any way with UI
        // So if dispose is sent during that operation this will lead to initialization
        // on already disposed composite, we don't want this at all
        // We can prevent this error by delaying dispose operation to allow Edge to finish its
        // initialization to be disposed properly
        if (browser != null) {
            super.dispose();
        } else {
            UIUtils.timerExec(1000, super::dispose);
        }
    }

    public String getImageType(int type) {
        switch (type) {
            case SWT.IMAGE_BMP:
            case SWT.IMAGE_OS2_BMP:
            case SWT.IMAGE_BMP_RLE:
                return ".bmp"; //$NON-NLS-1$
            case SWT.IMAGE_GIF:
                return ".gif"; //$NON-NLS-1$
            case SWT.IMAGE_ICO:
                return ".ico"; //$NON-NLS-1$
            case SWT.IMAGE_JPEG:
                return ".jpeg"; //$NON-NLS-1$
            case SWT.IMAGE_PNG:
                return ".png"; //$NON-NLS-1$
            case SWT.IMAGE_TIFF:
                return ".tiff"; //$NON-NLS-1$
            default:
                return ""; //$NON-NLS-1$
        }
    }

}
