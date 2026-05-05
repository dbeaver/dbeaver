/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public class CopyActiveQueryHandler extends AbstractHandler {
    private static final Log log = Log.getLog(CopyActiveQueryHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);

        if (editor != null && editor.getDocument() != null) {
            final SQLScriptElement query = editor.extractActiveQuery();

            if (query != null) {
                try {
                    UIUtils.setClipboardContents(
                        Display.getCurrent(),
                        TextTransfer.getInstance(),
                        editor.getDocument().get(query.getOffset(), query.getLength())
                    );
                } catch (BadLocationException e) {
                    log.warn("Can't extract query", e);
                }
            }
        }

        return null;
    }
}
