/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;


public abstract class AbstractSQLAction extends Action implements IEditorActionDelegate
{
    private SQLEditor editor;
    private IEditorPart targetEditor;

    protected abstract void execute(SQLEditor editor);

    public void run()
    {
        SQLEditor editor = getEditor();
        if (editor != null) {
            execute(editor);
        }
    }

    public void run(IAction action)
    {
        this.run();
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor)
    {
        this.targetEditor = targetEditor;
    }

    protected SQLEditor getEditor()
    {
        if (editor != null) {
            return editor;
        } else if (targetEditor instanceof SQLEditor) {
            return (SQLEditor)targetEditor;
        } else {
            return null;
        }
    }

    public void setProcessor(SQLEditor editor)
    {
        this.editor = editor;
    }
}