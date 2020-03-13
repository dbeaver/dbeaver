/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;

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
            Integer widgetCommand = TextEditorUtils.getTextEditorActionMap().get(commandID);
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
