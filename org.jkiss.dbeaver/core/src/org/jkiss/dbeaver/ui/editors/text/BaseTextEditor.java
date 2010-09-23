/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.texteditor.StatusTextEditor;

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

    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);
        // org.eclipse.ui.internal.ShowInMenu(id=viewsShowIn)
        //menu.remove(ITextEditorActionConstants.GROUP_OPEN);
        //menu.remove(ContributionItemFactory.VIEWS_SHOW_IN.getId());

    }

}