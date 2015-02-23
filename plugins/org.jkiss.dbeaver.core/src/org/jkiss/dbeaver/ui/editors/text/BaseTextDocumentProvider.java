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
