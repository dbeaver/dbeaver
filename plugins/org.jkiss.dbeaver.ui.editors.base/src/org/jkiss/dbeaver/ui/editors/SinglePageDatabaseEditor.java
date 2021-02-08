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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * SinglePageDatabaseEditor
 */
public abstract class SinglePageDatabaseEditor<INPUT_TYPE extends IEditorInput> extends AbstractDatabaseEditor<INPUT_TYPE>
{

    private ProgressEditorPart progressEditorPart;

    @Override
    public final void createPartControl(Composite parent) {
        final IEditorInput editorInput = getEditorInput();
        if (editorInput instanceof DatabaseLazyEditorInput) {
            createLazyEditorPart(parent, (DatabaseLazyEditorInput)editorInput);
        } else {
            createEditorControl(parent);
        }
    }

    private void createLazyEditorPart(Composite parent, final DatabaseLazyEditorInput input) {
        progressEditorPart = new ProgressEditorPart(this);
        progressEditorPart.init(getEditorSite(), input);
        progressEditorPart.createPartControl(parent);

        DatabaseEditorUtils.setPartBackground(this, parent);
    }

    @Override
    public void recreateEditorControl() {
        if (progressEditorPart != null) {
            Composite parent = progressEditorPart.destroyAndReturnParent();
            progressEditorPart = null;
            createEditorControl(parent);
            parent.layout(true, true);
        }
    }

    public abstract void createEditorControl(Composite parent);

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return getEditorInput() instanceof DBPContextProvider ? ((DBPContextProvider) getEditorInput()).getExecutionContext() : null;
    }

}