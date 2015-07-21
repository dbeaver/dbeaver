/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.resource.JFaceResources;
import org.jkiss.dbeaver.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditorStandalone;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.controls.imageview.ImageViewer;
import org.jkiss.dbeaver.ui.editors.binary.BinaryContent;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

/**
* ControlPanelEditor
*/
public class ContentPanelEditor extends BaseValueEditor<Control> implements IValueEditorStandalone {

    static final Log log = Log.getLog(ContentPanelEditor.class);

    public ContentPanelEditor(IValueController controller) {
        super(controller);
    }

    @Override
    public void showValueEditor()
    {
    }

    @Override
    public void closeValueEditor()
    {
    }

    @Override
    public void primeEditorValue(@Nullable final Object value) throws DBException
    {
        if (value == null) {
            log.warn("NULL content value. Must be DBDContent.");
            return;
        }
        DBeaverUI.runInUI(valueController.getValueSite().getWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Prime content value", 1);
                try {
                    DBDContent content = (DBDContent) value;
                    DBDContentStorage data = content.getContents(monitor);
                    if (control instanceof Text) {
                        monitor.subTask("Read text value");
                        Text text = (Text) control;
                        StringWriter buffer = new StringWriter();
                        if (data != null) {
                            Reader contentReader = data.getContentReader();
                            try {
                                ContentUtils.copyStreams(contentReader, -1, buffer, monitor);
                            } finally {
                                ContentUtils.close(contentReader);
                            }
                        }
                        text.setText(buffer.toString());
                    } else if (control instanceof HexEditControl) {
                        monitor.subTask("Read binary value");
                        HexEditControl hexEditControl = (HexEditControl) control;
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        if (data != null) {
                            InputStream contentStream = data.getContentStream();
                            try {
                                ContentUtils.copyStreams(contentStream, -1, buffer, monitor);
                            } catch (IOException e) {
                                ContentUtils.close(contentStream);
                            }
                        }
                        hexEditControl.setContent(buffer.toByteArray());
                    } else if (control instanceof ImageViewer) {
                        monitor.subTask("Read image value");
                        ImageViewer imageViewControl = (ImageViewer) control;
                        if (data != null) {
                            InputStream contentStream = data.getContentStream();
                            try {
                                if (!imageViewControl.loadImage(contentStream)) {
                                    valueController.showMessage("Can't load image: " + imageViewControl.getLastError().getMessage(), true);
                                } else {
                                    valueController.showMessage("Image: " + imageViewControl.getImageDescription(), false);
                                }
                            } finally {
                                ContentUtils.close(contentStream);
                            }
                        } else {
                            imageViewControl.clearImage();
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                    valueController.showMessage(e.getMessage(), true);
                } finally {
                    monitor.done();
                }
            }
        });
    }

    @Override
    public Object extractEditorValue() throws DBException
    {
        final DBDContent content = (DBDContent) valueController.getValue();
        if (content == null) {
            log.warn("NULL content value. Must be DBDContent.");
        } else {
            DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Read content value", 1);
                    try {
                        if (control instanceof Text) {
                            monitor.subTask("Read text value");
                            Text styledText = (Text) control;
                            content.updateContents(
                                monitor,
                                new StringContentStorage(styledText.getText()));
                        } else if (control instanceof HexEditControl) {
                            monitor.subTask("Read binary value");
                            HexEditControl hexEditControl = (HexEditControl) control;
                            BinaryContent binaryContent = hexEditControl.getContent();
                            ByteBuffer buffer = ByteBuffer.allocate((int) binaryContent.length());
                            try {
                                binaryContent.get(buffer, 0);
                            } catch (IOException e) {
                                log.error(e);
                            }
                            content.updateContents(
                                monitor,
                                new BytesContentStorage(buffer.array(), GeneralUtils.getDefaultFileEncoding()));
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
        }
        return content;
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        DBDContent content = (DBDContent) valueController.getValue();
        if (ContentUtils.isTextContent(content)) {
            Text text = new Text(editPlaceholder, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
            text.setEditable(!valueController.isReadOnly());
            text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
            return text;
        } else {
            ImageDetector imageDetector = new ImageDetector(content);
            if (!DBUtils.isNullValue(content)) {
                DBeaverUI.runInUI(valueController.getValueSite().getWorkbenchWindow(), imageDetector);
            }

            IContributionManager editBar = valueController.getEditBar();
            if (imageDetector.isImage()) {
                ImageViewer imageViewer = new ImageViewer(editPlaceholder, SWT.BORDER);
                if (editBar != null) {
                    imageViewer.fillToolBar(editBar);
                }
                return imageViewer;
            } else {
                final HexEditControl hexEditor = new HexEditControl(editPlaceholder, SWT.BORDER);
                if (editBar != null) {
                    editBar.add(new Action("Switch Insert/Overwrite mode", DBeaverIcons.getImageDescriptor(UIIcon.CURSOR)) {
                        @Override
                        public void run() {
                            hexEditor.redrawCaret(true);
                        }
                    });
                }
                return hexEditor;
            }
        }
    }

    private static class ImageDetector implements DBRRunnableWithProgress {
        private final DBDContent content;
        private boolean isImage;

        private ImageDetector(DBDContent content)
        {
            this.content = content;
        }

        public boolean isImage()
        {
            return isImage;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            if (!content.isNull()) {
                try {
                    InputStream contentStream = content.getContents(monitor).getContentStream();
                    try {
                        new ImageData(contentStream);
                    } finally {
                        ContentUtils.close(contentStream);
                    }
                    isImage = true;
                }
                catch (Exception e) {
                    // this is not an image
                }
            }
        }
    }
}
