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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ContentFormatHandler extends AbstractHandler {

    public ContentFormatHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor instanceof ContentEditor) {
            IEditorPart editorPart = ((ContentEditor) activeEditor).getActiveEditor();
            ITextViewer textViewer = editorPart.getAdapter(ITextViewer.class);
            if (textViewer instanceof TextViewer) {
                StyledText textWidget = textViewer.getTextWidget();
                boolean oldEditable = textWidget.getEditable();
                textWidget.setEditable(true);
                try {
                    if (((TextViewer) textViewer).canDoOperation(SourceViewer.FORMAT)) {
                        ((TextViewer) textViewer).doOperation(SourceViewer.FORMAT);
                    }
                } finally {
                    textWidget.setEditable(oldEditable);
                }

            }
        }
        return null;
    }


}
