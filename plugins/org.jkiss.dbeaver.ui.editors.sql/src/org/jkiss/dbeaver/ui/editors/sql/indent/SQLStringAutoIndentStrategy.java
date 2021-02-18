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
package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;


public class SQLStringAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy
{

    /**
     * Creates a new SQL string auto indent strategy for the given document partitioning.
     * 
     * @param partitioning the document partitioning
     */
    public SQLStringAutoIndentStrategy(String partitioning)
    {
        super();
    }

    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command)
    {

    }
}
