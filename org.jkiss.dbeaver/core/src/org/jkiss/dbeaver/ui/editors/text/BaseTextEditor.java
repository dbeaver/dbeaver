/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.ui.views.properties.PropertiesPage;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends TextEditor {

    public Object getAdapter(Class required)
    {
        if (required == IPropertySheetPage.class) {
            return new PropertiesPage();
        }
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