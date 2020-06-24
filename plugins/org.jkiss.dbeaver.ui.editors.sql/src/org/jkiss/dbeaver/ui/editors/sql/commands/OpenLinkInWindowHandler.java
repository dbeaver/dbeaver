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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenLinkInWindowHandler extends AbstractHandler implements IElementUpdater {

	private static final Log log = Log.getLog(OpenLinkInWindowHandler.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SQLEditor  editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
		if (editor == null) {
			log.debug("editor was null");
			return null;
		}
		
		ISelection selection = editor.getSelectionProvider().getSelection();
		if (selection.isEmpty() || !(selection instanceof TextSelection)) {
			log.debug("log was null");
			return null;
		}
		
		TextSelection textSelection = (TextSelection) selection;
		log.debug("The selected text is " + textSelection.getText());
		
		// TODO: should be constant
		// TODO: how to handle the spaces, handle url generation
		String googleLink = "http://www.google.com/search?q=" + textSelection.getText().replaceAll(" ", "%20");
		// It should not even be possible to use DBeaver on mobile
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI(googleLink));
			} catch (IOException | URISyntaxException e) {
				// TODO: handle the error
				this.log.error("error while opening the browser", e);
			}
		} else {
			log.debug("Desktop is not supported");
		}
		return null;
	}
    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setText(SQLEditorMessages.editors_sql_actions_search_at_google);
        element.setTooltip(SQLEditorMessages.editors_sql_actions_search_at_google_tip);
    }
}
