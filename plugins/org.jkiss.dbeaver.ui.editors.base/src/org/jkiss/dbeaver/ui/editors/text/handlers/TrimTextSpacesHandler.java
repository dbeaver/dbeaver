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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

public class TrimTextSpacesHandler extends AbstractTextHandler {

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Override
    public Object execute(ExecutionEvent executionEvent) throws ExecutionException {

        BaseTextEditor textEditor = BaseTextEditor.getTextEditor(HandlerUtil.getActiveEditor(executionEvent));

        if (textEditor != null) {
            ISelectionProvider provider = textEditor.getSelectionProvider();
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            if (provider != null && document != null) {
                ISelection selection = provider.getSelection();
                if (selection instanceof ITextSelection) {
                    ITextSelection textSelection = (ITextSelection) selection;
                    int offset = textSelection.getOffset();
                    if (textSelection.getLength() > 0) {
                        int startLine = textSelection.getStartLine();
                        int endLine = textSelection.getEndLine();
                        if (startLine != endLine) { // Highlighted more than one line - make trim for each row separately
                            try {
                                StringBuilder allStrings = new StringBuilder();
                                for (int i = startLine; i <= endLine; i++) {
                                    IRegion lineInformation = document.getLineInformation(i);
                                    String untrimmedString = document.get(lineInformation.getOffset(), lineInformation.getLength());
                                    allStrings.append(untrimmedString.trim());
                                    if (i != endLine) {
                                        allStrings.append(lineBreak);
                                    }
                                }
                                document.replace(offset, textSelection.getLength(), allStrings.toString());
                            } catch (BadLocationException e) {
                                DBWorkbench.getPlatformUI().showError("Trim spaces", "Error getting or replacing text", e);
                            }
                        } else { // Make trim only for the highlighted area of the string
                            String trimmedSelection = textSelection.getText().trim();
                            if (!CommonUtils.isEmpty(trimmedSelection)) {
                                try {
                                    document.replace(offset, textSelection.getLength(), trimmedSelection);
                                } catch (BadLocationException e) {
                                    DBWorkbench.getPlatformUI().showError("Trim spaces", "Error replacing text", e);
                                }
                            }
                        }
                    } else if (offset > 0) { // Nothing is highlighted - make trim only for the string on which the cursor is
                        try {
                            IRegion information = document.getLineInformationOfOffset(offset);
                            int startLine = information.getOffset();
                            int length = offset - startLine;
                            String untrimmedString = document.get(startLine, length);
                            String trimmedString = untrimmedString.trim();
                            if (!CommonUtils.isEmpty(untrimmedString)) {
                                document.replace(startLine, length, trimmedString);
                            }
                        } catch (BadLocationException e) {
                            DBWorkbench.getPlatformUI().showError("Trim spaces", "Error replacing text", e);
                        }
                    }
                }
            }
        }
        return null;
    }

}
