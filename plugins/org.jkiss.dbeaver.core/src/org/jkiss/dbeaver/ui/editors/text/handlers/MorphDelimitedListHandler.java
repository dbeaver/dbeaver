/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.utils.ArrayUtils;

public final class MorphDelimitedListHandler extends AbstractCommentHandler {

    @Override
    protected void processAction(ISelectionProvider selectionProvider, ICommentsSupport commentsSupport, IDocument document, ITextSelection textSelection) throws BadLocationException {
        System.out.println(textSelection.getText());
    }

}