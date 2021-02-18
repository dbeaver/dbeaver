/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
