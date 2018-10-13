/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.jkiss.dbeaver.ui.editors.EditorSearchActionsContributor;

/**
 * SQL Editor contributor
 */
public class ERDEditorContributor extends EditorActionBarContributor
{
    private ERDEditorPart activeEditorPart;


    public ERDEditorContributor()
    {
        super();

        createActions();
    }

    private void createActions()
    {
    }

    @Override
    public void dispose()
    {
        setActiveEditor(null);

        super.dispose();
    }

    @Override
    public void setActiveEditor(IEditorPart targetEditor)
    {
        super.setActiveEditor(targetEditor);

        if (activeEditorPart == targetEditor) {
            return;
        }
        activeEditorPart = (ERDEditorPart)targetEditor;

        if (activeEditorPart != null) {
            // Update editor actions


        }
        EditorSearchActionsContributor.registerSearchActions(activeEditorPart);
    }

    @Override
    public void init(IActionBars bars)
    {
        super.init(bars);
    }

    @Override
    public void contributeToMenu(IMenuManager manager)
    {
        super.contributeToMenu(manager);

        IMenuManager editMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            //editMenu.add(new Separator());
            //editMenu.add(new Separator());
            //editMenu.add(executeStatementAction);
            //editMenu.add(executeScriptAction);
        }
    }

    @Override
    public void contributeToCoolBar(ICoolBarManager manager)
    {
        super.contributeToCoolBar(manager);
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
    }

}
