/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDContentStorageLocal;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.impl.TemporaryContentStorage;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ContentEditorInput
 */
public class ContentEditorInput implements IFileEditorInput, IPathEditorInput, IDatabaseEditorInput
{
    static final Log log = LogFactory.getLog(ContentEditorInput.class);

    private DBDValueController valueController;
    private IContentEditorPart[] editorParts;
    private IFile contentFile;
    private boolean contentDetached = false;

    private DBNNode treeNode;

    ContentEditorInput(
        DBDValueController valueController,
        IContentEditorPart[] editorParts,
        DBRProgressMonitor monitor)
        throws DBException
    {
        this.valueController = valueController;
        this.editorParts = editorParts;
        this.prepareContent(monitor);
    }

    public DBDValueController getValueController()
    {
        return valueController;
    }

    IContentEditorPart[] getEditors()
    {
        return editorParts;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBIcon.TYPE_LOB.getImageDescriptor();
    }

    public String getName()
    {
        String tableName = valueController.getColumnMetaData().getTableName();
        String inputName = CommonUtils.isEmpty(tableName) ?
            valueController.getColumnMetaData().getName() :
            tableName + "." + valueController.getColumnMetaData().getName();
        if (isReadOnly()) {
            inputName += " [Read Only]";
        }
        return inputName;
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    public String getToolTipText()
    {
        return getName();
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == IFile.class) {
            return contentFile;
        }
        return null;
    }

    private DBDContent getContent()
        throws DBCException
    {
        Object value = valueController.getValue();
        if (value instanceof DBDContent) {
            return (DBDContent)value;
        } else {
            throw new DBCException("Value do not support streaming");
        }
    }

    private void prepareContent(DBRProgressMonitor monitor)
        throws DBException
    {
        DBDContent content = getContent();
        DBDContentStorage storage = content.getContents(monitor);
        if (storage instanceof DBDContentStorageLocal) {
            // User content's storage directly
            contentFile = ((DBDContentStorageLocal)storage).getDataFile();
            contentDetached = true;
        } else {
            // Copy content to local file
            try {
                // Create file
                contentFile = ContentUtils.createTempContentFile( monitor, valueController.getColumnId());

                // Write value to file
                if (!content.isNull()) {
                    copyContentToFile(content, monitor);
                }
            }
            catch (IOException e) {
                // Delete temp file
                if (contentFile != null && contentFile.exists()) {
                    try {
                        contentFile.delete(true, false, monitor.getNestedMonitor());
                    }
                    catch (CoreException e1) {
                        log.warn("Could not delete temporary content file", e);
                    }
                }
                throw new DBException(e);
            }
        }

        // Mark file as readonly
        if (valueController.isReadOnly()) {
            ResourceAttributes attributes = contentFile.getResourceAttributes();
            if (attributes != null) {
                attributes.setReadOnly(true);
                try {
                    contentFile.setResourceAttributes(attributes);
                }
                catch (CoreException e) {
                    throw new DBException(e);
                }
            }
        }
    }

    void release(DBRProgressMonitor monitor)
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

    public IPath getPath()
    {
        return contentFile == null ? null : contentFile.getFullPath();
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
                DBeaverUtils.makeMonitor(monitor));
        }
        catch (IOException e) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(e));
        }
    }

    void loadFromExternalFile(File extFile, IProgressMonitor monitor)
        throws CoreException
    {
        try {
            InputStream inputStream = new FileInputStream(extFile);
            try {
                ResourceAttributes atts = contentFile.getResourceAttributes();
                atts.setReadOnly(false);
                contentFile.setResourceAttributes(atts);
/*
                File intFile = contentFile.getLocation().toFile();
                OutputStream outputStream = new FileOutputStream(intFile);
                try {
                    ContentUtils.copyStreams(inputStream, extFile.length(), outputStream, DBeaverUtils.makeMonitor(monitor));
                }
                finally {
                    outputStream.close();
                }
*/
                // Append zero empty content to trigger content refresh
                contentFile.setContents(
                    inputStream,//new ByteArrayInputStream(new byte[0]),
                    true,
                    false,
                    monitor);
            }
            finally {
                inputStream.close();
            }
        }
        catch (Throwable e) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(e));
        }
    }

    private void copyContentToFile(DBDContent contents, DBRProgressMonitor monitor)
        throws DBException, IOException
    {
        DBDContentStorage storage = contents.getContents(monitor);
        if (contents.isNull() || storage == null) {
            log.warn("Could not copy null content");
            return;
        }

        if (ContentUtils.isTextContent(contents)) {
            ContentUtils.copyReaderToFile(monitor, storage.getContentReader(), storage.getContentLength(), storage.getCharset(), contentFile);
        } else {
            ContentUtils.copyStreamToFile(monitor, storage.getContentStream(), storage.getContentLength(), contentFile);
        }
    }

    void updateContentFromFile(IProgressMonitor monitor)
        throws DBException, IOException
    {
        if (valueController.isReadOnly()) {
            throw new DBCException("Could not update read-only value");
        }

        DBRProgressMonitor localMonitor = DBeaverUtils.makeMonitor(monitor);
        DBDContent content = getContent();
        DBDContentStorage storage = content.getContents(localMonitor);
        if (storage instanceof DBDContentStorageLocal) {
            // Nothing to update - we user content's storage
            contentDetached = true;
        } else {
            // Create new storage and pass it to content
            storage = new TemporaryContentStorage(contentFile);
            contentDetached = content.updateContents(localMonitor, storage);
        }
        valueController.updateValue(content);
    }

    ////////////////////////////////////////////////////////
    // IDatabaseEditorInput methods

    public DBPDataSource getDataSource() {
        return valueController.getDataSource();
    }

    public DBNNode getTreeNode() {
        if (treeNode == null) {
            treeNode = DBeaverCore.getInstance().getMetaModel().findNode(valueController.getDataSource().getContainer());
        }
        return treeNode;
    }

    public DBSObject getDatabaseObject() {
        DBNNode node = getTreeNode();
        return node == null ? null : node.getObject();
    }

    public String getDefaultPageId() {
        return null;
    }

}