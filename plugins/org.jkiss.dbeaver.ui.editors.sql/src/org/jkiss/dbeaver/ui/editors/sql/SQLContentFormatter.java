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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;

class SQLContentFormatter extends ContentFormatter implements IContentFormatterExtension {
    private static final Log log = Log.getLog(SQLContentFormatter.class);

    private final SQLEditorBase editor;

    SQLContentFormatter(SQLEditorBase editor) {
        this.editor = editor;
    }

    @Override
    public void format(IDocument document, IFormattingContext context) {
        IFormattingStrategy strategy = getFormattingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
        if (strategy == null) {
            return;
        }

        IRegion region;
        if (editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_ACTIVE_QUERY)) {
            SQLScriptElement activeQuery = editor.extractActiveQuery();
            if (activeQuery == null) {
                return;
            }
            region = new Region(activeQuery.getOffset(), activeQuery.getLength());
        } else {
            region = (IRegion) context.getProperty(FormattingContextProperties.CONTEXT_REGION);
            if (region == null) {
                region = new Region(0, document.getLength());
            }
        }

        try {
            String source = document.get(region.getOffset(), region.getLength());
            String formatted = strategy.format(source, false, null, null);
            document.replace(region.getOffset(), region.getLength(), formatted);
        } catch (BadLocationException e) {
            log.error("Error formatting the document", e);
        }
    }
}
