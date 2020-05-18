/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;


/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor extends MultiPageAbstractEditor implements IDatabaseEditor, DBPContextProvider
{
    public static final String PARAMETER_ACTIVE_PAGE = "activePage";
    public static final String PARAMETER_ACTIVE_FOLDER = "activeFolder";

    private DatabaseEditorListener listener;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
    }

    @Override
    public void dispose()
    {
        if (listener != null) {
            listener.dispose();
        }
        super.dispose();
    }

    @Override
    public IDatabaseEditorInput getEditorInput() {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);

        if (listener != null) {
            listener.dispose();
        }
        listener = new DatabaseEditorListener(this);
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return getEditorInput().getExecutionContext();
    }

    @Override
    protected void createPages()
    {
        super.createPages();
        DatabaseEditorUtils.setPartBackground(this, getContainer());
    }

    @Override
    protected void setContainerStyles() {
        super.setContainerStyles();
        Composite container = getContainer();
        if (container instanceof CTabFolder && !container.isDisposed()){
            CSSUtils.setCSSClass(container, DBStyles.COLORED_BY_CONNECTION_TYPE);
        }
    }


    @Override
    public boolean isActiveTask() {
        return false;
    }
}