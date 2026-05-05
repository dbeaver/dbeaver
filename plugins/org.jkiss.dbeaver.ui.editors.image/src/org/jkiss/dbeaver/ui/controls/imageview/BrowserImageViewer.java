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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;


/**
 * Image viewer control
 */
public class BrowserImageViewer extends AbstractImageViewer {
    private static final Log log = Log.getLog(BrowserImageViewer.class);

    private Browser browser;

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
        } catch (SWTError error) {
            if (error.code == SWT.ERROR_NOT_IMPLEMENTED) {
                browser = null;
                // HACK: Will force SWT to use IE instead. We can't use SWT.DEFAULT because it might resolve to SWT.EDGE
                browser = new Browser(this, SWT.WEBKIT);
            } else {
                for (Control control : getChildren()) {
                    control.dispose();
                }
                throw error;
            }
        } finally {
            browserCreating = false;
        }
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        browser.setJavascriptEnabled(false); // We don't need java script to open images


        // Add DND support


    }

    @Override
    public boolean loadImage(@NotNull InputStream inputStream) {
        boolean success = false;
        try {
            clearTempFile();
            if (RuntimeUtils.isLinux()) {
                tempFile = Files.createTempFile(
                    DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "dbeaver-images"),
                    "image",
                    ""
                );
                try (OutputStream outputStream = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    IOUtils.copyStream(inputStream, outputStream);
                    success = true;
                } catch (IOException ex) {
                    log.error("Error reading image data", ex);
                    showBinaryTXT(inputStream);
                }
            } else {
                try (ImageInputStream stream = ImageIO.createImageInputStream(inputStream)) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                    if (!readers.hasNext()) {
                        throw new IOException("No Image readers");
                    } else {
                        ImageReader reader = readers.next();
                        reader.setInput(stream);
                        tempFile = Files.createTempFile(
                            DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "dbeaver-images"),
                            "image",
                            "." + reader.getFormatName().toLowerCase(Locale.ROOT)
                        );
                        ImageIO.write(ImageIO.read(stream),
                            reader.getFormatName().toLowerCase(Locale.ROOT),
                            tempFile.toFile()
                        );
                        success = true;
                    }
                } catch (IOException exception) {
                    if (!exception.getMessage().equals("closed")) {
                        log.error("Error reading image data", exception);
                        showBinaryTXT(inputStream);
                    }
                }
            }

            URL url = tempFile.toUri().toURL();
            browser.setUrl(url.toString());
        } catch (IOException exception) {
            log.error(exception);
        }
        return success;
    }

    private void showBinaryTXT(@NotNull InputStream inputStream) throws IOException {
        tempFile = Files.createTempFile(
            DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "dbeaver-images"),
            "image",
            ".txt"
        );
        String s = IOUtils.readToString(new InputStreamReader(inputStream, GeneralUtils.DEFAULT_ENCODING));
        Files.writeString(tempFile, s);
    }

    @Override
    public boolean clearImage() {
        clearTempFile();
        browser.setUrl(null);
        return true;
    }

    @Override
    public void dispose() {
        clearTempFile();
        // Edge uses callbacks which have a lowest priority in UI
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
                        Thread.onSpinWait();
                    }
                    UIUtils.syncExec(BrowserImageViewer.super::dispose);
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    private void clearTempFile() {
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
                log.warn(e);
            }
        }
        tempFile = null;
    }

    @Nullable
    @Override
    public Path getExternalFilePath() {
        return tempFile;
    }
}
