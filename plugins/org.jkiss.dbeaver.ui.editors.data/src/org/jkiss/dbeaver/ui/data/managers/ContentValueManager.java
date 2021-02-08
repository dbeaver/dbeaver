/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.storage.ExternalContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPropertyManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.dialogs.TextViewDialog;
import org.jkiss.dbeaver.ui.data.editors.ContentInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;


/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 *
 * @author Serge Rider
 */
public class ContentValueManager extends BaseValueManager {

    private static final Log log = Log.getLog(ContentValueManager.class);

    public static final String PROP_CATEGORY_CONTENT = "CONTENT";

    public static void contributeContentActions(@NotNull IContributionManager manager, @NotNull final IValueController controller, final IValueEditor activeEditor)
            throws DBCException
    {
        if (controller.getValue() instanceof DBDContent) {
            if (!((DBDContent) controller.getValue()).isNull()) {
                manager.add(new Action(ResultSetMessages.model_jdbc_save_to_file_, DBeaverIcons.getImageDescriptor(UIIcon.SAVE_AS)) {
                    @Override
                    public void run() {
                        saveToFile(controller);
                    }
                });
            }
            // Logo can be changed
            manager.add(new Action("Open in external editor", DBeaverIcons.getImageDescriptor(UIIcon.DOTS_BUTTON)) {
                @Override
                public void run() {
                    try {
                        final Object value = controller.getValue();
                        if (value == null) {
                            DBWorkbench.getPlatformUI().showError("Data is empty", "Can not save null data value");
                        }
                        if (value instanceof DBDContent) {
                            getDBDContent(value);
                        } else {
                            String str = controller.getValueHandler()
                                    .getValueDisplayString(controller.getValueType(), 
                                            controller.getValue(), DBDDisplayFormat.EDIT);
                            String charset = 
                                    DBValueFormatting.getDefaultBinaryFileEncoding(controller.getExecutionContext().getDataSource());
                            byte[] bytes = str.getBytes(charset);
                            openOctetStream(bytes);
                        }
                    } catch (IOException e) {
                        DBWorkbench.getPlatformUI().showError("Open content", "Error while trying to open the value", e);
                    } catch (Exception e) {
                        DBWorkbench.getPlatformUI().showError("Error", 
                                "Unexpected error while trying to open the selected value", e);
                    }
                }
            });
            manager.add(new Action(ResultSetMessages.model_jdbc_load_from_file_, DBeaverIcons.getImageDescriptor(UIIcon.LOAD)) {
                @Override
                public void run() {
                    if (loadFromFile(controller)) {
                        if (activeEditor != null) {
                            try {
                                activeEditor.primeEditorValue(controller.getValue());
                            } catch (DBException e) {
                                DBWorkbench.getPlatformUI().showError("Load from file", "Error loading contents from file", e);
                            }
                        }
                    }
                }
            });
            manager.add(new Separator());
        }
    }

    private static void getDBDContent(Object value) throws IOException, DBCException {
        DBDContent content = (DBDContent) value;
        try {
            UIUtils.runInProgressService(monitor -> {
                String charset = null;
                DBDContentStorage storage;
                try {
                    storage = content.getContents(monitor);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    if (storage != null) {
                        try (InputStream inputStream = storage.getContentStream()) {
                            ContentUtils.copyStreams(inputStream, -1, buffer, monitor);
                        } catch (IOException e) {
                            DBWorkbench.getPlatformUI().showError("IOException", "File exception", e);
                        }
                        charset = storage.getCharset();
                    } else {
                        charset = DBValueFormatting.getDefaultBinaryFileEncoding(content.getDataSource());
                    }
                    byte[] byteData = buffer.toByteArray();
                    openOctetStream(byteData);
                } catch (DBCException e1) {
                    DBWorkbench.getPlatformUI().showError("DBCException", "Error reading contents", e1);
                } catch (IOException e) {
                    DBWorkbench.getPlatformUI().showError("IOException", "File exception while opening", e);
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            DBWorkbench.getPlatformUI().showError("Reading from content", "Error loading contents from file", e);
        }
    }

    private static void openOctetStream(byte[] data) throws IOException {
        File tmpFile = File.createTempFile("dbtmp", ".octet-stream");
        FileOutputStream fos = new FileOutputStream(tmpFile);
        if (data == null) {
            DBWorkbench.getPlatformUI().showError("Open Content", "Raw value was null");
            fos.close();
        }
        if (data.length == 0) {
            log.info("file has no content");
            fos.close();
            tmpFile.delete();
        } else {
            fos.write(data);
            fos.close();
            // use OS to open the file
            Desktop.getDesktop().open(tmpFile);
            // delete the file when the user closes the DBeaver application
            tmpFile.deleteOnExit();
        }
    }

    public static IValueEditor openContentEditor(@NotNull IValueController controller)
    {
        Object value = controller.getValue();
        IValueController.EditType binaryEditType = IValueController.EditType.valueOf(
                controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getString(ResultSetPreferences.RESULT_SET_BINARY_EDITOR_TYPE));
        if (controller.getValueType().getDataKind() == DBPDataKind.STRING) {
            // String
            return new TextViewDialog(controller);
        } else if (binaryEditType != IValueController.EditType.EDITOR && value instanceof DBDContentCached) {
            // Use string editor for cached content
            return new TextViewDialog(controller);
        } else if (value instanceof DBDContent) {
            return ContentEditor.openEditor(controller);
        } else {
            controller.showMessage(ResultSetMessages.model_jdbc_unsupported_content_value_type_, DBPMessageType.ERROR);
            return null;
        }
    }

    public static boolean loadFromFile(final IValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(ResultSetMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return false;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File openFile = DialogUtils.openFile(shell);
        if (openFile == null) {
            return false;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        UIUtils.runInUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), monitor -> {
            try {
                DBDContentStorage storage;
                if (ContentUtils.isTextContent(value)) {
                    storage = new ExternalContentStorage(DBWorkbench.getPlatform(), openFile, GeneralUtils.UTF8_ENCODING);
                } else {
                    storage = new ExternalContentStorage(DBWorkbench.getPlatform(), openFile);
                }
                value.updateContents(monitor, storage);
                controller.updateValue(value, true);
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        });
        return true;
    }

    public static void saveToFile(IValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(ResultSetMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File saveFile = DialogUtils.selectFileForSave(shell, controller.getValueName());
        if (saveFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    DBDContentStorage storage = value.getContents(monitor);
                    if (ContentUtils.isTextContent(value)) {
                        try (Reader cr = storage.getContentReader()) {
                            ContentUtils.saveContentToFile(
                                    cr,
                                    saveFile,
                                    GeneralUtils.UTF8_ENCODING,
                                    monitor
                                    );
                        }
                    } else {
                        try (InputStream cs = storage.getContentStream()) {
                            ContentUtils.saveContentToFile(cs, saveFile, monitor);
                        }
                    }
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
        catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                    ResultSetMessages.model_jdbc_could_not_save_content,
                    ResultSetMessages.model_jdbc_could_not_save_content_to_file_ + saveFile.getAbsolutePath() + "'", //$NON-NLS-2$
                    e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final IValueController controller, @Nullable IValueEditor activeEditor)
            throws DBCException
    {
        super.contributeActions(manager, controller, activeEditor);
        contributeContentActions(manager, controller, activeEditor);
    }

    @Override
    public void contributeProperties(@NotNull DBPPropertyManager propertySource, @NotNull IValueController controller)
    {
        super.contributeProperties(propertySource, controller);
        try {
            Object value = controller.getValue();
            if (value instanceof DBDContent) {
                propertySource.addProperty(
                        PROP_CATEGORY_CONTENT,
                        "content_type", //$NON-NLS-1$
                        ResultSetMessages.model_jdbc_content_type,
                        ((DBDContent)value).getContentType());
                final long contentLength = ((DBDContent) value).getContentLength();
                if (contentLength >= 0) {
                    propertySource.addProperty(
                            PROP_CATEGORY_CONTENT,
                            "content_length", //$NON-NLS-1$
                            ResultSetMessages.model_jdbc_content_length,
                            contentLength);
                }
            }
        }
        catch (Exception e) {
            log.warn("Can't extract CONTENT value information", e); //$NON-NLS-1$
        }
    }

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
            throws DBException
    {
        switch (controller.getEditType()) {
        case INLINE:
            // Open inline/panel editor
            Object value = controller.getValue();
            if (controller.getValueType().getDataKind() == DBPDataKind.STRING) {
                return new StringInlineEditor(controller);
            } else if (value instanceof DBDContentCached &&
                    ContentUtils.isTextValue(((DBDContentCached) value).getCachedValue()))
            {
                return new ContentInlineEditor(controller);
            } else {
                return null;
            }
        case EDITOR:
            return openContentEditor(controller);
        case PANEL:
            return new ContentPanelEditor(controller);
        default:
            return null;
        }
    }

}
