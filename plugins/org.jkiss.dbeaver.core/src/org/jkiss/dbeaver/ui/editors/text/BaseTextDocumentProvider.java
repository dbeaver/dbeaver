/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

import java.lang.reflect.InvocationTargetException;


/**
 * BaseTextDocumentProvider
 */
public abstract class BaseTextDocumentProvider extends AbstractDocumentProvider {

    protected BaseTextDocumentProvider()
    {

    }

    protected Document createEmptyDocument()
    {
        return new Document();
    }

    @Override
    protected IAnnotationModel createAnnotationModel(Object element) throws CoreException
    {
        return new ProjectionAnnotationModel();
    }

    @Override
    protected IRunnableContext getOperationRunner(final IProgressMonitor monitor)
    {
        return new IRunnableContext() {
            @Override
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
            {
                runnable.run(monitor);
            }
        };
    }

}
