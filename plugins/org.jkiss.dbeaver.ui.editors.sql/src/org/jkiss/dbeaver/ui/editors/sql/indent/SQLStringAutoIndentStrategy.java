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
package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;

public class SQLStringAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {

    private static final Log log = Log.getLog(SQLStringAutoIndentStrategy.class);
    private static final char DOLLAR_QUOTE_START = '$';

    private final String partitioning;

    /**
     * Creates a new SQL string auto indent strategy for the given document partitioning.
     * 
     * @param partitioning the document partitioning
     */
    public SQLStringAutoIndentStrategy(@NotNull String partitioning) {
        this.partitioning = partitioning;
    }

    @Override
    public void customizeDocumentCommand(@NotNull IDocument document, @NotNull DocumentCommand command) {
        if (command.offset < 0 || command.length != 0 || command.text == null
            || TextUtilities.equals(document.getLegalLineDelimiters(), command.text) < 0
        ) {
            return;
        }
        try {
            ITypedRegion region = TextUtilities.getPartition(document, partitioning, command.offset, true);
            // Dollar-quoted function bodies are string partitions, but should retain indentation when editing code.
            if (SQLParserPartitions.CONTENT_TYPE_SQL_STRING.equals(region.getType())
                && region.getLength() > 0
                && document.getChar(region.getOffset()) == DOLLAR_QUOTE_START
            ) {
                super.customizeDocumentCommand(document, command);
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }
    }
}
