/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentationExtension;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.debug.core.model.DatabaseThread;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugTarget;

public class DatabaseDebugModelPresentation extends LabelProvider implements IDebugModelPresentationExtension {

    private final Map<String, Object> attributes = new HashMap<>();

    private final ILabelProvider labelProvider;

    public DatabaseDebugModelPresentation()
    {
        this(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    }

    public DatabaseDebugModelPresentation(ILabelProvider labelProvider)
    {
        this.labelProvider = labelProvider;
    }

    public Object getAttribute(String attribute)
    {
        return attributes.get(attribute);
    }

    @Override
    public void setAttribute(String attribute, Object value)
    {
        attributes.put(attribute, value);
    }

    @Override
    public Image getImage(Object element)
    {
        return labelProvider.getImage(element);
    }

    @Override
    public String getText(Object element)
    {
        // FIXME:AF: register adapters
        try {
            if (element instanceof DatabaseThread) {
                DatabaseThread thread = (DatabaseThread) element;
                return thread.getName();
            }
            if (element instanceof DatabaseProcess) {
                DatabaseProcess process = (DatabaseProcess) element;
                return process.getLabel();
            }
            if (element instanceof IDatabaseDebugTarget) {
                IDatabaseDebugTarget databaseDebugTarget = (IDatabaseDebugTarget) element;
                return databaseDebugTarget.getName();
            }
        } catch (CoreException e) {
            return "<not responding>";
        }
        return labelProvider.getText(element);
    }

    @Override
    public void dispose()
    {
        attributes.clear();
        super.dispose();
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener)
    {
    }

    @Override
    public IEditorInput getEditorInput(Object element)
    {
        return null;
    }

    @Override
    public String getEditorId(IEditorInput input, Object element)
    {
        return null;
    }

    @Override
    public boolean requiresUIThread(Object element)
    {
        return false;
    }

}
