package org.jkiss.dbeaver.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

/**
 * Command director
 */
public class WidgetCommandDirector implements IHandler {

    @Override
    public void addHandlerListener(IHandlerListener handlerListener)
    {

    }

    @Override
    public void dispose()
    {

    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        String commandID = event.getCommand().getId();
        Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control instanceof Text) {
            Text text = (Text)control;
            if (ITextEditorActionDefinitionIds.LINE_START.equals(commandID) ||
                ITextEditorActionDefinitionIds.TEXT_START.equals(commandID)) {
                text.setSelection(0);
            } else if (ITextEditorActionDefinitionIds.LINE_END.equals(commandID) ||
                ITextEditorActionDefinitionIds.TEXT_END.equals(commandID)) {
                text.setSelection(text.getCharCount());
            }
        } else if (control instanceof StyledText) {
            Integer widgetCommand = BaseTextEditor.getActionMap().get(commandID);
            StyledText text = (StyledText)control;
            if (widgetCommand != null) {
                text.invokeAction(widgetCommand);
            }
        }

        return null;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public boolean isHandled()
    {
        return true;
    }

    @Override
    public void removeHandlerListener(IHandlerListener handlerListener)
    {

    }

}
