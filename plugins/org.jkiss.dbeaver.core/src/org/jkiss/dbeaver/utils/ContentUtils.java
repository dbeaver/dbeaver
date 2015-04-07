/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.utils;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentXMLEditorPart;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content manipulation utilities
 */
public class ContentUtils {

    static final int STREAM_COPY_BUFFER_SIZE = 10000;

    static final Log log = Log.getLog(ContentUtils.class);
    public static final String DEFAULT_FILE_CHARSET_NAME = "UTF-8";

    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    public static final Charset DEFAULT_FILE_CHARSET = UTF8_CHARSET;

    private static final Map<String, byte[]> BOM_MAP = new HashMap<String, byte[]>();

    static {
        BOM_MAP.put("UTF-8", new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF} );
        BOM_MAP.put("UTF-16", new byte[] {(byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-16BE", new byte[] {(byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE} );
        BOM_MAP.put("UTF-32", new byte[] { 0x0, 0x0, (byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-32BE", new byte[] { 0x0, 0x0, (byte) 0xFE, (byte) 0xFF} );
        BOM_MAP.put("UTF-32LE", new byte[] { (byte) 0xFE, (byte) 0xFF, 0x0, 0x0} );
    }

    static final char[] HEX_CHAR_TABLE = {
      '0', '1', '2', '3',
      '4', '5', '6', '7',
      '8', '9', 'a', 'b',
      'c', 'd', 'e', 'f'
    };
    private static String curDialogFolder = System.getProperty("user.dir");

    public static String getCurDialogFolder()
    {
        return curDialogFolder;
    }

    public static void setCurDialogFolder(String curDialogFolder)
    {
        ContentUtils.curDialogFolder = curDialogFolder;
    }

    public static byte[] getCharsetBOM(String charsetName)
    {
        return BOM_MAP.get(charsetName.toUpperCase());
    }

    public static void writeByteAsHex(Writer out, byte b) throws IOException
    {
        int v = b & 0xFF;
        out.write(HEX_CHAR_TABLE[v >>> 4]);
        out.write(HEX_CHAR_TABLE[v & 0xF]);
    }

    public static void writeBytesAsHex(Writer out, byte[] buf, int off, int len) throws IOException
    {
        for (int i = 0; i < len; i++) {
            byte b = buf[off + i];
            int v = b & 0xFF;
            out.write(HEX_CHAR_TABLE[v >>> 4]);
            out.write(HEX_CHAR_TABLE[v & 0xF]);
        }
    }

    public static String getDefaultFileEncoding()
    {
        return System.getProperty("file.encoding", DEFAULT_FILE_CHARSET_NAME);
    }

    public static String getDefaultConsoleEncoding()
    {
        String consoleEncoding = System.getProperty("console.encoding");
        if (CommonUtils.isEmpty(consoleEncoding)) {
            consoleEncoding = getDefaultFileEncoding();
        }
        return consoleEncoding;
    }

    public static String getDefaultLineSeparator()
    {
        return System.getProperty("line.separator", "\n");
    }

    public static IFile createTempContentFile(DBRProgressMonitor monitor, String fileName)
        throws IOException
    {
        IFile file = makeTempFile(
            monitor,
            DBeaverCore.getInstance().getLobFolder(monitor.getNestedMonitor()),
            fileName,
            "data");
        try {
            file.setCharset(getDefaultBinaryFileEncoding(null), monitor.getNestedMonitor());
        } catch (CoreException e) {
            log.error("Can't set file charset", e);
        }
        return file;
    }

    public static IFile makeTempFile(DBRProgressMonitor monitor, IFolder folder, String name, String extension)
        throws IOException
    {
        IFile tempFile = folder.getFile(name + "-" + System.currentTimeMillis() + "." + extension);  //$NON-NLS-1$ //$NON-NLS-2$
        try {
            InputStream contents = new ByteArrayInputStream(new byte[0]);
            tempFile.create(contents, true, monitor.getNestedMonitor());
        } catch (CoreException ex) {
            throw new IOException(MessageFormat.format(CoreMessages.DBeaverCore_error_can_create_temp_file, tempFile.toString(), folder.toString()), ex);
        }
        return tempFile;
    }

    public static void deleteTempFile(DBRProgressMonitor monitor, IFile file)
    {
        try {
            file.delete(true, false, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Could not delete temporary file '" + file.getFullPath().toString() + "'", e);
        }
    }

    public static File selectFileForSave(Shell parentShell)
    {
        return selectFileForSave(parentShell, "Save Content As", null, null);
    }

    public static File selectFileForSave(Shell parentShell, String title, String[] filterExt, @Nullable String fileName)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.SAVE);
        fileDialog.setText(title);
        fileDialog.setOverwrite(true);
        if (filterExt != null) {
            fileDialog.setFilterExtensions(filterExt);
        }
        if (fileName != null) {
            fileDialog.setFileName(fileName);
        }

        fileName = openFileDialog(fileDialog);
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        final File saveFile = new File(fileName);
        File saveDir = saveFile.getParentFile();
        if (!saveDir.exists()) {
            UIUtils.showErrorDialog(parentShell, "Bad file name", "Directory '" + saveDir.getAbsolutePath() + "' does not exists");
            return null;
        }
        return saveFile;
    }

    public static File openFile(Shell parentShell)
    {
        return openFile(parentShell, null);
    }

    public static File openFile(Shell parentShell, String[] filterExt)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN);
        if (filterExt != null) {
            fileDialog.setFilterExtensions(filterExt);
        }
        String fileName = openFileDialog(fileDialog);
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        final File loadFile = new File(fileName);
        if (!loadFile.exists()) {
            MessageBox aMessageBox = new MessageBox(parentShell, SWT.ICON_WARNING | SWT.OK);
            aMessageBox.setText("File doesn't exists");
            aMessageBox.setMessage("The file "+ loadFile.getAbsolutePath() + " doesn't exists.");
            aMessageBox.open();
            return null;
        }
        return loadFile;
    }

    public static String openFileDialog(FileDialog fileDialog)
    {
        if (curDialogFolder == null) {
            fileDialog.setFilterPath(curDialogFolder);
        }
        String fileName = fileDialog.open();
        if (!CommonUtils.isEmpty(fileName)) {
            curDialogFolder = fileDialog.getFilterPath();
        }
        return fileName;
    }

    public static void saveContentToFile(InputStream contentStream, File file, DBRProgressMonitor monitor)
        throws IOException
    {
        try {
            OutputStream os = new FileOutputStream(file);
            try {
                copyStreams(contentStream, file.length(), os, monitor);
            }
            finally {
                os.close();
            }
            // Check for cancel
            if (monitor.isCanceled()) {
                // Delete output file
                if (!file.delete()) {
                    log.warn("Could not delete incomplete file '" + file.getAbsolutePath() + "'");
                }
            }
        }
        finally {
            close(contentStream);
        }
    }

    public static void saveContentToFile(Reader contentReader, File file, String charset, DBRProgressMonitor monitor)
        throws IOException
    {
        try {
            Writer writer = new OutputStreamWriter(
                new FileOutputStream(file),
                charset);

            try {
                copyStreams(contentReader, file.length(), writer, monitor);
            }
            finally {
                writer.close();
            }
            // Check for cancel
            if (monitor.isCanceled()) {
                // Delete output file
                if (!file.delete()) {
                    log.warn("Could not delete incomplete file '" + file.getAbsolutePath() + "'");
                }
            }
        }
        finally {
            contentReader.close();
        }
    }

    public static void copyStreams(
        InputStream inputStream,
        long contentLength,
        OutputStream outputStream,
        DBRProgressMonitor monitor)
        throws IOException
    {
        monitor.beginTask("Copy binary content", contentLength < 0 ? STREAM_COPY_BUFFER_SIZE : (int) contentLength);
        try {
            byte[] buffer = new byte[STREAM_COPY_BUFFER_SIZE];
            for (;;) {
                if (monitor.isCanceled()) {
                    break;
                }
                int count = inputStream.read(buffer);
                if (count <= 0) {
                    break;
                }
                outputStream.write(buffer, 0, count);
                monitor.worked(STREAM_COPY_BUFFER_SIZE);
            }
        }
        finally {
            monitor.done();
        }
    }

    public static void copyStreams(
        Reader reader,
        long contentLength,
        Writer writer,
        DBRProgressMonitor monitor)
        throws IOException
    {
        monitor.beginTask("Copy character content", contentLength < 0 ? STREAM_COPY_BUFFER_SIZE : (int) contentLength);
        try {
            char[] buffer = new char[STREAM_COPY_BUFFER_SIZE];
            for (;;) {
                if (monitor.isCanceled()) {
                    break;
                }
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                writer.write(buffer, 0, count);
                monitor.worked(STREAM_COPY_BUFFER_SIZE);
            }
        }
        finally {
            monitor.done();
        }
    }

    public static long calculateContentLength(
        File file,
        String charset)
        throws IOException
    {
        return calculateContentLength(
            new FileInputStream(file),
            charset);
    }

    public static long calculateContentLength(
        InputStream stream,
        String charset)
        throws IOException
    {
        return calculateContentLength(
            new InputStreamReader(
                stream,
                charset));
    }

    public static long calculateContentLength(
        Reader reader)
        throws IOException
    {
        try {
            long length = 0;
            char[] buffer = new char[STREAM_COPY_BUFFER_SIZE];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                length += count;
            }
            return length;
        }
        finally {
            reader.close();
        }
    }

    public static void close(Closeable closeable)
    {
        try {
            closeable.close();
        }
        catch (IOException e) {
            log.warn("Error closing stream", e);
        }
    }

    public static void copyStreamToFile(DBRProgressMonitor monitor, InputStream inputStream, long contentLength, IFile localFile)
        throws IOException
    {
        //localFile.appendContents(inputStream, true, false, monitor.getNestedMonitor());
        File file = localFile.getLocation().toFile();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            try {
                ContentUtils.copyStreams(inputStream, contentLength, outputStream, monitor);
            }
            finally {
                outputStream.close();
            }
        }
        finally {
            inputStream.close();
        }
        syncFile(monitor, localFile);

    }

    public static void copyReaderToFile(DBRProgressMonitor monitor, Reader reader, long contentLength, String charset, IFile localFile)
        throws IOException
    {
        try {
            if (charset == null) {
                charset = localFile.getCharset();
            } else {
                localFile.setCharset(charset, monitor.getNestedMonitor());
            }
        }
        catch (CoreException e) {
            log.warn("Can't set content charset", e);
        }
        File file = localFile.getLocation().toFile();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            try {
                Writer writer = new OutputStreamWriter(outputStream, charset);
                ContentUtils.copyStreams(reader, contentLength, writer, monitor);
                writer.flush();
            }
            finally {
                outputStream.close();
            }
        }
        finally {
            reader.close();
        }
        syncFile(monitor, localFile);
    }

    public static void syncFile(DBRProgressMonitor monitor, IResource localFile) {
        // Sync file with contents
        try {
            localFile.refreshLocal(IFile.DEPTH_ZERO, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Could not synchronize file '" + localFile + "' with contents", e);
        }
    }

    public static IFile getUniqueFile(IFolder folder, String fileName, String fileExt)
    {
        IFile file = folder.getFile(fileName + "." + fileExt);
        int index = 1;
        while (file.exists()) {
            file = folder.getFile(fileName + "-" + index + "." + fileExt);
            index++;
        }
        return file;
    }

    public static String readFileToString(File file) throws IOException
    {
        InputStream fileStream = new FileInputStream(file);
        try {
            UnicodeReader unicodeReader = new UnicodeReader(fileStream, ContentUtils.DEFAULT_FILE_CHARSET_NAME);
            StringBuilder result = new StringBuilder((int) file.length());
            char[] buffer = new char[4000];
            for (;;) {
                int count = unicodeReader.read(buffer);
                if (count <= 0) {
                    break;
                }
                result.append(buffer, 0, count);
            }
            return result.toString();
        } finally {
            ContentUtils.close(fileStream);
        }
    }

    public static String readToString(InputStream is, String charset) throws IOException
    {
        return readToString(new UnicodeReader(is, charset));
    }

    public static String readToString(Reader is) throws IOException
    {
        try {
            StringBuilder result = new StringBuilder(4000);
            char[] buffer = new char[4000];
            for (;;) {
                int count = is.read(buffer);
                if (count <= 0) {
                    break;
                }
                result.append(buffer, 0, count);
            }
            return result.toString();
        } finally {
            ContentUtils.close(is);
        }
    }

    @Nullable
    public static MimeType getMimeType(String contentType)
    {
        MimeType mimeType = null;
        if (contentType != null) {
            try {
                mimeType = new MimeType(contentType);
            } catch (MimeTypeParseException e) {
                log.error("Invalid content MIME type", e);
            }
        }
        return mimeType;
    }

    @Nullable
    public static IFile getFileFromEditorInput(IEditorInput editorInput)
    {
        try {
            Method getFileMethod = editorInput.getClass().getMethod("getFile");
            if (IFile.class.isAssignableFrom(getFileMethod.getReturnType())) {
                return IFile.class.cast(getFileMethod.invoke(editorInput));
            }
        } catch (Exception e) {
            log.debug("Error getting file from editor input with reflection", e);
            // Just ignore
        }
        if (editorInput instanceof IPathEditorInput && ((IPathEditorInput) editorInput).getPath() != null) {
            return convertPathToWorkspaceFile(((IPathEditorInput) editorInput).getPath());
        } else {
            return null;
        }
    }

    @Nullable
    public static IFile convertPathToWorkspaceFile(IPath path)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = root.getFileForLocation(path);
        if (file != null) {
            return file;
        }
        // Probably we have a path to some linked resource
        IPath folderPath = path.removeLastSegments(1);
        URI folderURI = folderPath.toFile().toURI();
        IContainer[] containers = root.findContainersForLocationURI(folderURI);
        if (!ArrayUtils.isEmpty(containers)) {
            IContainer container = containers[0];
            file = container.getFile(path.removeFirstSegments(path.segmentCount() - 1));
        }
        return file;
    }

    @Nullable
    public static IPath convertPathToWorkspacePath(IPath path)
    {
        IFile wFile = convertPathToWorkspaceFile(path);
        return wFile == null ? null : wFile.getFullPath();
    }

    public static String getDefaultBinaryFileEncoding(DBPDataSource dataSource)
    {
        IPreferenceStore preferenceStore;
        if (dataSource == null) {
            preferenceStore = DBeaverCore.getGlobalPreferenceStore();
        } else {
            preferenceStore = dataSource.getContainer().getPreferenceStore();
        }
        String fileEncoding = preferenceStore.getString(DBeaverPreferences.CONTENT_HEX_ENCODING);
        if (CommonUtils.isEmpty(fileEncoding)) {
            fileEncoding = getDefaultFileEncoding();
        }
        return fileEncoding;
    }

    public static String convertToString(byte[] bytes, int offset, int length)
    {
        char[] chars = new char[length];
        for (int i = offset; i < offset + length; i++) {
            int b = bytes[i];
            if (b < 0) {
                b = -b + 127;
            }
            chars[i - offset] = (char) b;
        }
        return new String(chars);
    }

    /**
     * Converts string to byte array.
     * This is loosy algorithm because it gets only first byte from each char.
     *
     * @param strValue
     * @return
     */
    public static byte[] convertToBytes(String strValue)
    {
        int length = strValue.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int c = strValue.charAt(i) & 255;
            if (c > 127) {
                c = -(c - 127);
            }
            bytes[i] = (byte)c;
        }
        return bytes;
    }

    public static DBDValueEditor openContentEditor(@NotNull DBDValueController controller)
    {
        Object value = controller.getValue();
        DBDValueController.EditType binaryEditType = DBDValueController.EditType.valueOf(
            controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE));
        if (binaryEditType != DBDValueController.EditType.EDITOR && value instanceof DBDContentCached) {
            // Use string editor for cached content
            return new TextViewDialog(controller);
        } else if (value instanceof DBDContent) {
            DBDContent content = (DBDContent)value;
            boolean isText = ContentUtils.isTextContent(content);
            List<ContentEditorPart> parts = new ArrayList<ContentEditorPart>();
            if (isText) {
                parts.add(new ContentTextEditorPart());
                if (isXML(content)) {
                    parts.add(new ContentXMLEditorPart());
                }
            } else {
                parts.add(new ContentBinaryEditorPart());
                parts.add(new ContentTextEditorPart());
                parts.add(new ContentImageEditorPart());
            }
            return ContentEditor.openEditor(
                controller,
                parts.toArray(new ContentEditorPart[parts.size()]));
        } else {
            controller.showMessage(CoreMessages.model_jdbc_unsupported_content_value_type_, true);
            return null;
        }
    }

    public static boolean isTextContent(DBDContent content)
    {
        String contentType = content == null ? null : content.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("text");
    }

    private static boolean isXML(DBDContent content)
    {
        return MimeTypes.TEXT_XML.equalsIgnoreCase(content.getContentType());
    }

    public static void contributeContentActions(@NotNull IContributionManager manager, @NotNull final DBDValueController controller)
        throws DBCException
    {
        if (controller.getValue() instanceof DBDContent && !((DBDContent)controller.getValue()).isNull()) {
            manager.add(new Action(CoreMessages.model_jdbc_save_to_file_, DBIcon.SAVE_AS.getImageDescriptor()) {
                @Override
                public void run() {
                    saveToFile(controller);
                }
            });
        }
        manager.add(new Action(CoreMessages.model_jdbc_load_from_file_, DBIcon.LOAD.getImageDescriptor()) {
            @Override
            public void run() {
                loadFromFile(controller);
            }
        });
    }

    private static void loadFromFile(final DBDValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(CoreMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File openFile = ContentUtils.openFile(shell);
        if (openFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        DBeaverUI.runInUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                try {
                    DBDContentStorage storage;
                    if (ContentUtils.isTextContent(value)) {
                        storage = new ExternalContentStorage(openFile, ContentUtils.DEFAULT_FILE_CHARSET_NAME);
                    } else {
                        storage = new ExternalContentStorage(openFile);
                    }
                    value.updateContents(monitor, storage);
                    controller.updateValue(value);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }

    private static void saveToFile(DBDValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(CoreMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File saveFile = ContentUtils.selectFileForSave(shell);
        if (saveFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBDContentStorage storage = value.getContents(monitor);
                        if (ContentUtils.isTextContent(value)) {
                            ContentUtils.saveContentToFile(
                                storage.getContentReader(),
                                saveFile,
                                ContentUtils.DEFAULT_FILE_CHARSET_NAME,
                                monitor
                            );
                        } else {
                            ContentUtils.saveContentToFile(
                                storage.getContentStream(),
                                saveFile,
                                monitor
                            );
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                shell,
                CoreMessages.model_jdbc_could_not_save_content,
                CoreMessages.model_jdbc_could_not_save_content_to_file_ + saveFile.getAbsolutePath() + "'", //$NON-NLS-2$
                e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

}
