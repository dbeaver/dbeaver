/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.project;

import org.eclipse.core.filebuffers.manipulation.ContainerCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;


/**
 * ProjectDocumentProvider
 */
public class ProjectDocumentProvider extends AbstractDocumentProvider {

    private static final int DEFAULT_BUFFER_SIZE = 10000;

    @Override
    protected Document createDocument(Object element) throws CoreException
    {
        if (element instanceof ProjectEditorInput) {
            Document document = new Document();
            if (setDocumentContent(document, (ProjectEditorInput) element)) {
                return document;
            }
        }

        throw new IllegalArgumentException("Project document provider supports only project editor input");
    }

    @Override
    protected IAnnotationModel createAnnotationModel(Object element) throws CoreException
    {
        return new ProjectionAnnotationModel();
    }

    @Override
    public boolean isReadOnly(Object element)
    {
        if (element instanceof ProjectEditorInput) {
            return ((ProjectEditorInput)element).getFile().isReadOnly();
        }
        return super.isReadOnly(element);
    }
    
    @Override
    public boolean isModifiable(Object element)
    {
        return !isReadOnly(element);
    }

    public boolean isDeleted(Object element) {

        if (element instanceof ProjectEditorInput) {
            ProjectEditorInput input= (ProjectEditorInput) element;
            IPath path= input.getFile().getLocation();
            return path == null || !path.toFile().exists();

        }
        return super.isDeleted(element);
    }

    @Override
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException
    {
        ProjectEditorInput input = (ProjectEditorInput) element;
        String encoding = ContentUtils.DEFAULT_FILE_CHARSET;

        IFile file = input.getFile();

        Charset charset;
        try {
            charset = Charset.forName(encoding);
        } catch (Exception ex) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(ex));
        }

        CharsetEncoder encoder = charset.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        InputStream stream;

        try {
            byte[] bytes;
            ByteBuffer byteBuffer = encoder.encode(CharBuffer.wrap(document.get()));
            if (byteBuffer.hasArray()) {
                bytes = byteBuffer.array();
            } else {
                bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes);
            }
            stream = new ByteArrayInputStream(bytes, 0, byteBuffer.limit());
        } catch (CharacterCodingException ex) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(ex));
        }

        if (file.exists()) {

            // inform about the upcoming content change
            fireElementStateChanging(element);
            try {
                file.setContents(stream, overwrite, true, monitor);
            } catch (CoreException x) {
                // inform about failure
                fireElementStateChangeFailed(element);
                throw x;
            } catch (RuntimeException x) {
                // inform about failure
                fireElementStateChangeFailed(element);
                throw x;
            }

        } else {
            try {
                monitor.beginTask("Save file '" + file.getName() + "'", 2000);
                ContainerCreator creator = new ContainerCreator(file.getWorkspace(), file.getParent().getFullPath());
                creator.createContainer(new SubProgressMonitor(monitor, 1000));
                file.create(stream, false, new SubProgressMonitor(monitor, 1000));
            }
            finally {
                monitor.done();
            }
        }
    }

    @Override
    protected IRunnableContext getOperationRunner(final IProgressMonitor monitor)
    {
        return new IRunnableContext() {
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
            {
                runnable.run(monitor);
            }
        };
    }


    protected boolean setDocumentContent(IDocument document, ProjectEditorInput editorInput) throws CoreException
    {
        IFile file = editorInput.getFile();
        InputStream contentStream = file.getContents(false);
        try {
            setDocumentContent(document, contentStream, null);
        } catch (IOException ex) {
            throw new CoreException(DBeaverUtils.makeExceptionStatus(ex));
        } finally {
            ContentUtils.close(contentStream);
        }
        return true;
    }

    protected void setDocumentContent(IDocument document, InputStream contentStream, String encoding) throws IOException
    {

        Reader in = null;

        try {

            if (encoding == null) {
                encoding = ContentUtils.DEFAULT_FILE_CHARSET;
            }

            in = new BufferedReader(new InputStreamReader(contentStream, encoding), DEFAULT_BUFFER_SIZE);
            StringBuffer buffer = new StringBuffer(DEFAULT_BUFFER_SIZE);
            char[] readBuffer = new char[2048];
            int n = in.read(readBuffer);
            while (n > 0) {
                buffer.append(readBuffer, 0, n);
                n = in.read(readBuffer);
            }

            document.set(buffer.toString());

        } finally {
            if (in != null) {
                ContentUtils.close(in);
            } else {
                ContentUtils.close(contentStream);
            }
        }
    }
}