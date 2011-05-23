/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends StatusTextEditor {

    public Object getAdapter(Class required)
    {
/*
        if (required == IPropertySheetPage.class) {
            return new PropertiesPage();
        }
*/
        return super.getAdapter(required);
    }

    @Override
    public void createPartControl(Composite parent)
    {
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());

        super.createPartControl(parent);
    }

    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);
        // org.eclipse.ui.internal.ShowInMenu(id=viewsShowIn)
        //menu.remove(ITextEditorActionConstants.GROUP_OPEN);
        //menu.remove(ContributionItemFactory.VIEWS_SHOW_IN.getId());

    }

    public void enableUndoManager(boolean enable)
    {
        TextViewer textViewer = (TextViewer) getSourceViewer();
        if (!enable) {
            textViewer.getUndoManager().disconnect();
        } else {
            textViewer.getUndoManager().connect(textViewer);
        }
    }

}