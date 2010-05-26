/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

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
import org.jkiss.dbeaver.model.data.DBDStreamHandler;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.IOException;

/**
 * LOBEditorInput
 */
public class LOBEditorInput implements IFileEditorInput, IPathEditorInput //IDatabaseEditorInput
{
    static Log log = LogFactory.getLog(LOBEditorInput.class);

    private DBDValueController valueController;
    private IFile lobFile;

    public LOBEditorInput(DBDValueController valueController, IProgressMonitor monitor)
        throws CoreException
    {
        this.valueController = valueController;
        this.saveDataToFile(monitor);
    }

    public DBDValueController getValueController()
    {
        return valueController;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBIcon.LOB.getImageDescriptor();
    }

    public String getName()
    {
        String tableName = valueController.getColumnMetaData().getTableName();
        return CommonUtils.isEmpty(tableName) ?
            valueController.getColumnMetaData().getColumnName() :
            tableName + "." + valueController.getColumnMetaData().getColumnName();
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    public String getToolTipText()
    {
        return "LOB column editor";
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == IFile.class) {
            return lobFile;
        }
        return null;
    }

    private void saveDataToFile(IProgressMonitor monitor)
        throws CoreException
    {
        try {
            DBDStreamHandler streamHandler;
            if (valueController.getValueHandler() instanceof DBDStreamHandler) {
                streamHandler = (DBDStreamHandler) valueController.getValueHandler();
            } else {
                throw new DBCException("Value do not support streaming");
            }

            // Construct file name
            String fileName;
            try {
                fileName = makeFileName();
            } catch (DBException e) {
                throw new CoreException(
                    DBeaverUtils.makeExceptionStatus(e));
            }
            // Create file
            lobFile = DBeaverCore.getInstance().makeTempFile(
                fileName,
                "data",
                monitor);

            // Write value to file
            Object value = valueController.getValue();
            if (value != null) {
                lobFile.setContents(
                    streamHandler.getContentStream(value),
                    true,
                    false,
                    monitor);
            }

            // Mark file as readonly
            if (valueController.isReadOnly()) {
                ResourceAttributes attributes = lobFile.getResourceAttributes();
                if (attributes != null) {
                    attributes.setReadOnly(true);
                    lobFile.setResourceAttributes(attributes);
                }
            }

        }
        catch (DBCException e) {
            throw new CoreException(
                DBeaverUtils.makeExceptionStatus(e));
        }
        catch (IOException e) {
            throw new CoreException(
                DBeaverUtils.makeExceptionStatus(e));
        }
    }

    private String makeFileName() throws DBException {
        StringBuilder fileName = new StringBuilder(valueController.getColumnId());
        if (valueController.getValueLocator() != null) {
            fileName.append(valueController.getValueLocator().getKeyId(valueController.getRow()));
        }
        return fileName.toString();
    }

    void release(IProgressMonitor monitor)
    {
        if (lobFile != null) {
            try {
                lobFile.delete(true, false, monitor);
            }
            catch (CoreException e) {
                log.warn(e);
            }
            //lobFile = null;
        }
    }

    public IFile getFile() {
        return lobFile;
    }

    public IStorage getStorage()
        throws CoreException
    {
        return lobFile;
    }

    public IPath getPath()
    {
        return lobFile == null ? null : lobFile.getLocation();
    }

    public boolean isReadOnly() {
        return valueController.isReadOnly();
    }
}