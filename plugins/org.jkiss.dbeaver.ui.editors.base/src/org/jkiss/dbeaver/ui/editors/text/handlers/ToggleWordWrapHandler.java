package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

public class ToggleWordWrapHandler extends AbstractTextHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        // get active editor where word wrap will be toggled
        BaseTextEditor textEditor = BaseTextEditor.getTextEditor(HandlerUtil.getActiveEditor(event));

        if (textEditor != null) {
            // editor (ITextEditor) adapter returns StyledText
            Object text = textEditor.getAdapter(Control.class);
            if (text instanceof StyledText) {
                StyledText styledText = (StyledText) text;

                // toggle wrapping
                styledText.setWordWrap(!styledText.getWordWrap());
            }
        }

        return null;
    }

}