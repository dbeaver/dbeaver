/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

/**
 * This class contains all of the shared functionality for comment handlers
 */
public abstract class AbstractCommentHandler extends AbstractTextHandler {

    static protected final Log log = Log.getLog(AbstractCommentHandler.class);

    public final Object execute(ExecutionEvent event) throws ExecutionException {
        BaseTextEditor textEditor = BaseTextEditor.getTextEditor(HandlerUtil.getActiveEditor(event));
        if (textEditor != null) {
            ICommentsSupport commentsSupport = textEditor.getCommentsSupport();
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            if (document != null && commentsSupport != null) {
                // get current text selection
                ISelectionProvider provider = textEditor.getSelectionProvider();
                if (provider != null) {
                    ISelection selection = provider.getSelection();
                    if (selection instanceof ITextSelection) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        if (!textSelection.isEmpty()) {
                            try {
                                processAction(textEditor.getSelectionProvider(), commentsSupport, document, textSelection);
                            } catch (BadLocationException e) {
                                log.warn(e);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    protected abstract void processAction(ISelectionProvider selectionProvider, ICommentsSupport commentsSupport, IDocument document, ITextSelection textSelection) throws BadLocationException;

}
