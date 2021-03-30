/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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