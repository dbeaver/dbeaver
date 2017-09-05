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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;

/**
 * SQLScriptPosition
*
* @author Serge Rider
*/
public class SQLScriptPosition extends Position implements Comparable<SQLScriptPosition> {

    private final ProjectionAnnotation foldingAnnotation;
    private final boolean isMultiline;

    public SQLScriptPosition(int offset, int length, boolean isMultiline, ProjectionAnnotation foldingAnnotation)
    {
        super(offset, length);
        this.foldingAnnotation = foldingAnnotation;
        this.isMultiline = isMultiline;
    }

    public ProjectionAnnotation getFoldingAnnotation()
    {
        return foldingAnnotation;
    }

    public boolean isMultiline() {
        return isMultiline;
    }

    @Override
    public int compareTo(SQLScriptPosition o)
    {
        return offset - o.offset;
    }
}
