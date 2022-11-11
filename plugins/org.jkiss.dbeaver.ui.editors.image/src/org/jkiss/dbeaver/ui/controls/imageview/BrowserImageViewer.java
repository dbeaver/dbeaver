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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;


/**
 * Image viewer control
 */
public class BrowserImageViewer extends AbstractImageViewer {

    private final Browser browser;

    private volatile boolean browserCreating = false;
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

        browserCreating = true;
        try {
            browser = new Browser(this, SWT.NONE);
        } finally {
            browserCreating = false;
        }
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        browser.setJavascriptEnabled(false); // We don't need java script to open images


        // Add DND support


    }

    @Override
    public boolean loadImage(InputStream inputStream) {
        try {
            ImageInputStream stream = ImageIO.createImageInputStream(inputStream);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            reader.setInput(stream);

            tempFile = Files.createTempFile("image", "." + reader.getFormatName().toLowerCase(Locale.ROOT));
            ImageIO.write(ImageIO.read(stream), reader.getFormatName().toLowerCase(Locale.ROOT), tempFile.toFile());
            URL url = tempFile.toUri().toURL();
            browser.setUrl(url.toString());
        } catch (IOException exception) {
            exception.printStackTrace();
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
        // We can prevent this error by delaying dispose in independent thread operation to allow Edge to finish its
        // initialization to be disposed properly
        //FIXME That should be removed as soon as Edge will be fixed, this is an awfull hack
        if (browser != null) {
            super.dispose();
        } else {
            new Job("Disposing browser") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    while (browserCreating) {

                    }
                    UIUtils.syncExec(BrowserImageViewer.super::dispose);
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

}
