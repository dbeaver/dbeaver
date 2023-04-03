/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class SQLEditorHandlerExpandCollapseAllFoldings extends AbstractHandler {
    private static final String PREFIX = "org.jkiss.dbeaver.ui.editors.sql.";
    private static final String SUFFIX = "AllFoldings";
    private static final String EXPAND_COMMAND_ID = PREFIX + "Expand" + SUFFIX;
    private static final String COLLAPSE_COMMAND_ID = PREFIX + "Collapse" + SUFFIX;

    @Override
    public Object execute(ExecutionEvent event) {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor == null) {
            return null;
        }
        SQLEditorBase sqlEditor = activeEditor.getAdapter(SQLEditorBase.class);
        if (sqlEditor == null || !sqlEditor.isFoldingEnabled()) {
            return null;
        }
        ProjectionAnnotationModel model = sqlEditor.getProjectionAnnotationModel();
        if (model == null) {
            return null;
        }
        IDocument document = sqlEditor.getDocument();
        if (document == null) {
            return null;
        }
        int length = sqlEditor.getDocument().getLength();
        String commandId = event.getCommand().getId();
        if (EXPAND_COMMAND_ID.equals(commandId)) {
            model.expandAll(0, length);
        } else if (COLLAPSE_COMMAND_ID.equals(commandId)) {
            model.collapseAll(0, length);
        }
        return null;
    }
}
