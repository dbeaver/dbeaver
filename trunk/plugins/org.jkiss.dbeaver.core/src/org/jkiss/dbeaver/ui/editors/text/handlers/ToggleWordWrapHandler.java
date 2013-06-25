package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public class ToggleWordWrapHandler extends AbstractTextHandler {

    public Object execute(ExecutionEvent event) {
        // get active editor where word wrap will be toggled
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

        if (activePart instanceof ITextEditor) {
            ITextEditor editor = (ITextEditor) activePart;

            // editor (ITextEditor) adapter returns StyledText
            Object text = editor.getAdapter(Control.class);
            if (text instanceof StyledText) {
                StyledText styledText = (StyledText) text;

                // toggle wrapping
                styledText.setWordWrap(!styledText.getWordWrap());
            }
        }

        return null;
    }

}