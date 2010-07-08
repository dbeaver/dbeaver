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
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentBinary;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueListener;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * LOBEditorInput
 */
public class ContentEditorInput implements IFileEditorInput, IPathEditorInput //IDatabaseEditorInput
{
    static Log log = LogFactory.getLog(ContentEditorInput.class);

    private DBDValueController valueController;
    private IContentEditorPart[] editorParts;
    private IFile contentFile;

    ContentEditorInput(
        DBDValueController valueController,
        IContentEditorPart[] editorParts,
        IProgressMonitor monitor)
        throws CoreException
    {
        this.valueController = valueController;
        this.editorParts = editorParts;
        this.saveDataToFile(monitor);
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

    private void saveDataToFile(IProgressMonitor monitor)
        throws CoreException
    {
        try {
            DBDContent content = getContent();

            // Construct file name
            String fileName;
            try {
                fileName = makeFileName();
            } catch (DBException e) {
                throw new CoreException(
                    DBeaverUtils.makeExceptionStatus(e));
            }
            // Create file
            contentFile = DBeaverCore.getInstance().makeTempFile(
                fileName,
                "data",
                monitor);

            // Write value to file
            Object value = valueController.getValue();
            if (value != null) {
                //Object contents = content.getContents(value);
                long contentLength = content.getContentLength();
                copyContentToFile(content, contentLength, monitor);
            }

            // Mark file as readonly
            if (valueController.isReadOnly()) {
                ResourceAttributes attributes = contentFile.getResourceAttributes();
                if (attributes != null) {
                    attributes.setReadOnly(true);
                    contentFile.setResourceAttributes(attributes);
                }
            }

        }
        catch (DBException e) {
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
        //if (valueController.getValueLocator() != null) {
        //    fileName.append(valueController.getValueLocator().getKeyId(valueController.getRow()));
        //}
        return fileName.toString();
    }

    void release(IProgressMonitor monitor)
    {
        if (contentFile != null) {
            try {
                contentFile.delete(true, false, monitor);
            }
            catch (CoreException e) {
                log.warn(e);
            }
            //contentFile = null;
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
                File intFile = contentFile.getLocation().toFile();
                OutputStream outputStream = new FileOutputStream(intFile);
                try {
                    ContentUtils.copyStreams(inputStream, extFile.length(), outputStream, DBeaverUtils.makeMonitor(monitor));
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
            throw new CoreException(DBeaverUtils.makeExceptionStatus(e));
        }
    }

    private void copyContentToFile(DBDContent contents, long contentLength, IProgressMonitor monitor)
        throws DBException, IOException
    {
        File file = contentFile.getLocation().toFile();

        if (contents instanceof DBDContentBinary) {
            InputStream inputStream = ((DBDContentBinary)contents).getContents();
            try {
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    ContentUtils.copyStreams(inputStream, contentLength, outputStream, DBeaverUtils.makeMonitor(monitor));
                }
                finally {
                    outputStream.close();
                }
            }
            finally {
                inputStream.close();
            }
        } else if (contents instanceof DBDContentCharacter) {
            Reader reader = ((DBDContentCharacter)contents).getContents();
            try {
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    String charset = ((DBDContentCharacter)contents).getCharset();
                    if (charset == null) {
                        charset = ContentUtils.DEFAULT_FILE_CHARSET;
                    }
                    Writer writer = new OutputStreamWriter(outputStream, charset);
                    ContentUtils.copyStreams(reader, contentLength, writer, DBeaverUtils.makeMonitor(monitor));
                    writer.flush();
                    try {
                        contentFile.setCharset(charset, monitor);
                    }
                    catch (CoreException e) {
                        log.warn(e);
                    }
                }
                finally {
                    outputStream.close();
                }
            }
            finally {
                reader.close();
            }
        } else {
            throw new DBCException("Unsupported content stream type: " + contents);
        }
    }

    void updateContentFromFile(IProgressMonitor monitor, DBDValueListener listener)
        throws DBException, IOException
    {
        if (valueController.isReadOnly()) {
            throw new DBCException("Could not update read-only value");
        }

        DBRProgressMonitor localMonitor = DBeaverUtils.makeMonitor(monitor);
        DBDContent content = getContent();
        File file = contentFile.getLocation().toFile();

        InputStream inputStream = new FileInputStream(file);
        try {
            if (content instanceof DBDContentBinary) {
                ((DBDContentBinary)content).updateContents(
                    valueController,
                    inputStream,
                    file.length(),
                    localMonitor,
                    listener);
            } else if (content instanceof DBDContentCharacter) {
                String charset;
                try {
                    charset = contentFile.getCharset();
                }
                catch (CoreException e) {
                    log.warn(e);
                    charset = ContentUtils.DEFAULT_FILE_CHARSET;
                }

                long contentLength = ContentUtils.calculateContentLength(file, charset);
                Reader reader = new InputStreamReader(inputStream, charset);
                ((DBDContentCharacter)content).updateContents(
                    valueController,
                    reader,
                    contentLength,
                    localMonitor,
                    listener);
            }
        }
        finally {
            inputStream.close();
        }

    }

}