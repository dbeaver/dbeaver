/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.List;

/**
 * LOBEditorInput
 */
public class LOBEditorInput implements IFileEditorInput //IDatabaseEditorInput
{
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
        // Construct file name
        String fileName;
        try {
            fileName = makeFileName();
        } catch (DBException e) {
            throw new CoreException(
                DBeaverUtils.makeExceptionStatus(e));
        }

        lobFile = DBeaverCore.getInstance().makeTempFile(
            fileName,
            "data",
            monitor);
    }

    private String makeFileName() throws DBException {
        String dsName = valueController.getSession().getDataSource().getContainer().getName();
        String catalogName = valueController.getColumnMetaData().getCatalogName();
        String schemaName = valueController.getColumnMetaData().getSchemaName();
        String tableName = valueController.getColumnMetaData().getTableName();
        String columnName = valueController.getColumnMetaData().getColumnName();
        StringBuilder fileName = new StringBuilder(CommonUtils.escapeIdentifier(dsName));
        if (!CommonUtils.isEmpty(catalogName)) {
            fileName.append('.').append(CommonUtils.escapeIdentifier(catalogName));
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            fileName.append('.').append(CommonUtils.escapeIdentifier(schemaName));
        }
        if (!CommonUtils.isEmpty(tableName)) {
            fileName.append('.').append(CommonUtils.escapeIdentifier(tableName));
        }
        if (!CommonUtils.isEmpty(columnName)) {
            fileName.append('.').append(CommonUtils.escapeIdentifier(columnName));
        }
        List<? extends DBCColumnMetaData> keyColumns = valueController.getValueLocator().getKeyColumns();
        for (DBCColumnMetaData keyColumn : keyColumns) {
            fileName.append('.').append(CommonUtils.escapeIdentifier(keyColumn.getColumnName()));
            Object keyValue = valueController.getColumnValue(keyColumn);
            fileName.append('-');
            fileName.append(CommonUtils.escapeIdentifier(keyValue == null ? "NULL" : keyValue.toString()));
        }
        return fileName.toString();
    }

    void release(IProgressMonitor monitor)
        throws CoreException
    {
        if (lobFile != null) {
            lobFile.delete(true, false, monitor);
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
}