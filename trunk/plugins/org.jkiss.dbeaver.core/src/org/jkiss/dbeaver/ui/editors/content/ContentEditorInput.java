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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.*;

/**
 * ContentEditorInput
 */
public class ContentEditorInput implements IPathEditorInput, DBPContextProvider
{
    static final Log log = Log.getLog(ContentEditorInput.class);

    private IValueController valueController;
    private ContentEditorPart[] editorParts;
    private IFile contentFile;
    private boolean contentDetached = false;

    public ContentEditorInput(
        IValueController valueController,
        ContentEditorPart[] editorParts,
        DBRProgressMonitor monitor)
        throws DBException
    {
        this.valueController = valueController;
        this.editorParts = editorParts;
        this.prepareContent(monitor);
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

    ContentEditorPart[] getEditors()
    {
        return editorParts;
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
    public Object getAdapter(Class adapter)
    {
        return null;
    }

    public DBDContent getContent()
        throws DBCException
    {
        Object value = valueController.getValue();
        if (value instanceof DBDContent) {
            return (DBDContent)value;
        } else {
            throw new DBCException("Value doesn't support streaming");
        }
    }

    private void prepareContent(DBRProgressMonitor monitor)
        throws DBException
    {
        DBDContent content = getContent();
        DBDContentStorage storage = content.getContents(monitor);

        if (contentDetached) {
            release(monitor);
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
                    try {
                        contentFile.delete(true, false, monitor.getNestedMonitor());
                    }
                    catch (CoreException e1) {
                        log.warn("Can't delete temporary content file", e);
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
        ResourceAttributes attributes = contentFile.getResourceAttributes();
        if (attributes != null && attributes.isReadOnly() != readOnly) {
            attributes.setReadOnly(readOnly);
            try {
                contentFile.setResourceAttributes(attributes);
            }
            catch (CoreException e) {
                throw new DBException("Can't set content read-only", e);
            }
        }
    }

    public void release(DBRProgressMonitor monitor)
    {
        if (contentFile != null && !contentDetached) {
            ContentUtils.deleteTempFile(monitor, contentFile);
            contentDetached = true;
        }
    }

    public IFile getFile() {
        return contentFile;
    }

    public IStorage getStorage()
        throws CoreException
    {
        return contentFile;
    }

    @Nullable
    @Override
    public IPath getPath()
    {
        return contentFile == null ? null : contentFile.getLocation();
    }

    public boolean isReadOnly() {
        return valueController.isReadOnly();
    }

    void saveToExternalFile(File file, IProgressMonitor monitor)
        throws CoreException
    {
        try {
            ContentUtils.saveContentToFile(
                contentFile.getContents(true),
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
            InputStream inputStream = new FileInputStream(extFile);
            try {
/*
                ResourceAttributes atts = contentFile.getResourceAttributes();
                atts.setReadOnly(false);
                contentFile.setResourceAttributes(atts);
*/

                File intFile = contentFile.getLocation().toFile();
                OutputStream outputStream = new FileOutputStream(intFile);
                try {
                    ContentUtils.copyStreams(inputStream, extFile.length(), outputStream, RuntimeUtils.makeMonitor(monitor));
                }
                finally {
                    outputStream.close();
                }

                // Append zero empty content to trigger content refresh
                contentFile.appendContents(
                    new ByteArrayInputStream(new byte[0]),
                    true,
                    false,
                    monitor);
            }
            finally {
                inputStream.close();
            }
        }
        catch (Throwable e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    private void copyContentToFile(DBDContent contents, DBRProgressMonitor monitor)
        throws DBException, IOException
    {
        DBDContentStorage storage = contents.getContents(monitor);

        markReadOnly(false);

        if (contents.isNull()) {
            ContentUtils.copyStreamToFile(monitor, new ByteArrayInputStream(new byte[0]), 0, contentFile);
        } else {
            if (storage == null) {
                log.warn("Can't get data from null storage");
                return;
            }
            if (ContentUtils.isTextContent(contents)) {
                ContentUtils.copyReaderToFile(monitor, storage.getContentReader(), storage.getContentLength(), storage.getCharset(), contentFile);
            } else {
                ContentUtils.copyStreamToFile(monitor, storage.getContentStream(), storage.getContentLength(), contentFile);
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
        DBDContent content = getContent();
        DBDContentStorage storage = content.getContents(localMonitor);
        if (storage instanceof DBDContentStorageLocal) {
            // Nothing to update - we user content's storage
            contentDetached = true;
        } else {
            // Create new storage and pass it to content
            storage = new TemporaryContentStorage(DBeaverCore.getInstance(), contentFile);
            contentDetached = content.updateContents(localMonitor, storage);
        }
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return valueController.getExecutionContext();
    }
}