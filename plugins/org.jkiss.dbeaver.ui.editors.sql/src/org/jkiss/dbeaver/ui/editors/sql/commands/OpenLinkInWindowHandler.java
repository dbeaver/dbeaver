package org.jkiss.dbeaver.ui.editors.sql.commands;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenLinkInWindowHandler extends AbstractHandler implements IElementUpdater {

    private static final String TITLE = "Search selection in web";
    private static final String SEARCH_WEB_ADDRESS_PREFIX = "http://www.google.com/search?q=";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            DBWorkbench.getPlatformUI().showError(TITLE, "No suitable editor was found for SQL");
            return null;
        }

        ISelection selection = editor.getSelectionProvider().getSelection();
        if (isSelectedTextNullOrEmpty(selection)) {
            DBWorkbench.getPlatformUI().showError(TITLE, "No text was selected");
            return null;
        }

        TextSelection textSelection = (TextSelection) selection;
        // TODO: how to handle the spaces, handle url generation
        String googleLink = SEARCH_WEB_ADDRESS_PREFIX + textSelection.getText().replaceAll(" ", "%20");
        // It should not even be possible to use DBeaver on mobile
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(googleLink));
            } catch (IOException | URISyntaxException e) {
                DBWorkbench.getPlatformUI().showError(TITLE, "Exception when searching.", e);
            }
        } else {
            DBWorkbench.getPlatformUI().showError(TITLE, "Desktop is not supported.");
        }
        return null;
    }
    
    private boolean isSelectedTextNullOrEmpty(ISelection selection) {
        if (selection == null) {
            return true;
        }
        
        if (selection.isEmpty() ||  !(selection instanceof TextSelection)) {
            return true;
        }
        
        TextSelection textSelection = (TextSelection)selection;
        String selectedText = textSelection.getText();
        return selectedText.isBlank() || selectedText.isEmpty();
    }
    
    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setText(SQLEditorMessages.editors_sql_actions_search_selected_text_online);
        element.setTooltip(SQLEditorMessages.editors_sql_actions_search_selected_text_online_tip);
    }
}
