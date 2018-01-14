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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDContentStorageLocal;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.impl.data.StringContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.LocalFileStorage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;

/**
 * ContentEditorInput
 */
public class ContentEditorInput implements IPathEditorInput, DBPContextProvider, IEncodingSupport
{
    private static final Log log = Log.getLog(ContentEditorInput.class);

    private IValueController valueController;
    private IEditorPart[] editorParts;
    private IEditorPart defaultPart;
    private File contentFile;
    private boolean contentDetached = false;
    private String fileCharset;

    public ContentEditorInput(
        IValueController valueController,
        IEditorPart[] editorParts,
        IEditorPart defaultPart,
        DBRProgressMonitor monitor)
        throws DBException
    {
        this.valueController = valueController;
        this.editorParts = editorParts;
        this.defaultPart = defaultPart;
        this.fileCharset = getDefaultEncoding();
        this.prepareContent(monitor);
    }

    public File getContentFile() {
        return contentFile;
    }

    public IValueController getValueController()
    {
        return valueController;
    }

    public void refreshContent(DBRProgressMonitor monitor, IValueController valueController) throws DBException
    {
        this.valueController = valueController;
        this.prepareContent(monitor);
    }

    IEditorPart[] getEditors()
    {
        return editorParts;
    }

    public IEditorPart getDefaultEditor() {
        return defaultPart;
    }

    @Override
    public boolean exists()
    {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        return DBeaverIcons.getImageDescriptor(DBIcon.TYPE_LOB);
    }

    @Override
    public String getName()
    {
        String inputName;
        if (valueController instanceof IAttributeController) {
            inputName = ((IAttributeController) valueController).getColumnId();
        } else {
            inputName = valueController.getValueName();
        }
        if (isReadOnly()) {
            inputName += " [Read Only]";
        }
        return inputName;
    }

    @Nullable
    @Override
    public IPersistableElement getPersistable()
    {
        return null;
    }

    @Override
    public String getToolTipText()
    {
        return getName();
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == IStorage.class) {
            return adapter.cast(new LocalFileStorage(contentFile, fileCharset));
        }
        return null;
    }

    public Object getValue() throws DBCException {
        return valueController.getValue();
    }

    private DBDContent getContent() throws DBCException {
        Object value = getValue();
        if (value instanceof DBDContent) {
            return (DBDContent) value;
        } else if (value instanceof String) {
            return new StringContent(valueController.getExecutionContext().getDataSource(), (String)value);
        } else {
            throw new DBCException("Unsupported content value type: " + value);
        }
    }

    private void prepareContent(DBRProgressMonitor monitor)
        throws DBException
    {
        DBDContent content = getContent();
        DBDContentStorage storage = content.getContents(monitor);

        if (contentDetached) {
            release();
            contentDetached = false;
        }
        if (storage instanceof DBDContentStorageLocal) {
            // User content's storage directly
            contentFile = ((DBDContentStorageLocal)storage).getDataFile();
            contentDetached = true;
        } else {
            // Copy content to local file
            try {
                // Create file
                if (contentFile == null) {
                    String valueId;
                    if (valueController instanceof IAttributeController) {
                        valueId = ((IAttributeController) valueController).getColumnId();
                    } else {
                        valueId = valueController.getValueName();
                    }

                    contentFile = ContentUtils.createTempContentFile(monitor, DBeaverCore.getInstance(), valueId);
                }

                // Write value to file
                copyContentToFile(content, monitor);
            }
            catch (IOException e) {
                // Delete temp file
                if (contentFile != null && contentFile.exists()) {
                    if (!contentFile.delete()) {
                        log.warn("Can't delete temporary content file '" + contentFile.getAbsolutePath() + "'");
                    }
                }
                throw new DBException("Can't delete content file", e);
            }
        }

        // Mark file as readonly
        if (valueController.isReadOnly()) {
            markReadOnly(true);
        }
    }

    private void markReadOnly(boolean readOnly) throws DBException
    {
        if (!contentFile.setWritable(!readOnly)) {
            throw new DBException("Can't set content read-only");
        }
    }

    public void release()
    {
        if (contentFile != null && !contentDetached) {
            if (!contentFile.delete()) {
                log.warn("Can't delete temp file '" + contentFile.getAbsolutePath() + "'");
            }
            contentDetached = true;
        }
    }

    @Nullable
    @Override
    public IPath getPath()
    {
        return contentFile == null ? null : new Path(contentFile.getAbsolutePath());
    }

    public boolean isReadOnly() {
        return valueController.isReadOnly();
    }

    void saveToExternalFile(File file, IProgressMonitor monitor)
        throws CoreException
    {
        try (InputStream is = new FileInputStream(contentFile)) {
            ContentUtils.saveContentToFile(
                is,
                file,
                RuntimeUtils.makeMonitor(monitor));
        }
        catch (IOException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    void loadFromExternalFile(File extFile, IProgressMonitor monitor)
        throws CoreException
    {
        try {
            release();
            contentFile = extFile;
            contentDetached = true;
            Object value = getValue();
            if (value instanceof DBDContent) {
                ((DBDContent)value).updateContents(
                    new DefaultProgressMonitor(monitor),
                    new ExternalContentStorage(DBeaverCore.getInstance(), extFile));
            } else {
                updateStringValueFromFile(extFile);
            }
            refreshContentParts(extFile);
        }
        catch (Throwable e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    private void updateStringValueFromFile(File extFile) throws DBException {
        try (FileReader is = new FileReader(extFile)) {
            String str = IOUtils.readToString(is);
            valueController.updateValue(str, false);
        } catch (IOException e) {
            throw new DBException("Error reading content from file", e);
        }
    }

    void refreshContentParts(Object source) {
        // Refresh editor parts
        for (IEditorPart cePart : editorParts) {
            if (cePart instanceof IRefreshablePart) {
                ((IRefreshablePart) cePart).refreshPart(source, false);
            }
        }
    }

    private void copyContentToFile(DBDContent contents, DBRProgressMonitor monitor)
        throws DBException, IOException
    {
        DBDContentStorage storage = contents.getContents(monitor);

        markReadOnly(false);

        try (OutputStream os = new FileOutputStream(contentFile)) {
            if (contents.isNull()) {
                ContentUtils.copyStreams(new ByteArrayInputStream(new byte[0]), 0, os, monitor);
            } else {
                if (storage == null) {
                    log.warn("Can't get data from null storage");
                    return;
                }
                try (InputStream is = storage.getContentStream()) {
                    ContentUtils.copyStreams(is, storage.getContentLength(), os, monitor);
                }
            }
        }

        markReadOnly(valueController.isReadOnly());
    }

    public void updateContentFromFile(IProgressMonitor monitor)
        throws DBException
    {
        if (valueController.isReadOnly()) {
            throw new DBCException("Can't update read-only value");
        }

        DBRProgressMonitor localMonitor = RuntimeUtils.makeMonitor(monitor);
        Object value = getValue();
        if (value instanceof DBDContent) {
            DBDContent content = (DBDContent) value;
            DBDContentStorage storage = content.getContents(localMonitor);
            if (storage instanceof DBDContentStorageLocal) {
                // Nothing to update - we user content's storage
                contentDetached = true;
            } else if (storage instanceof DBDContentCached) {
                // Create new storage and pass it to content
                try (FileInputStream is = new FileInputStream(contentFile)) {
                    if (storage instanceof StringContentStorage) {
                        try (Reader reader = new InputStreamReader(is, fileCharset)) {
                            storage = StringContentStorage.createFromReader(reader);
                        }
                    } else {
                        storage = BytesContentStorage.createFromStream(is, contentFile.length(), fileCharset);
                    }
                    //StringContentStorage.
                    contentDetached = content.updateContents(localMonitor, storage);
                } catch (IOException e) {
                    throw new DBException("Error reading content from file", e);
                }
            } else {
                // Create new storage and pass it to content
                storage = new TemporaryContentStorage(DBeaverCore.getInstance(), contentFile, fileCharset);
                contentDetached = content.updateContents(localMonitor, storage);
            }
        } else {
            // Just read as string
            updateStringValueFromFile(contentFile);
            contentDetached = true;
        }
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return valueController.getExecutionContext();
    }

    public String getEncoding() {
        return fileCharset;
    }

    @Override
    public String getDefaultEncoding() {
        return DBValueFormatting.getDefaultBinaryFileEncoding(valueController.getExecutionContext().getDataSource());
    }

    public void setEncoding(String fileCharset) {
        this.fileCharset = fileCharset;
        for (IEditorPart part : editorParts) {
            try {
                part.init(part.getEditorSite(), this);
            } catch (PartInitException e) {
                log.error("Error refreshing content editor part " + part, e);
            }
        }
    }
}