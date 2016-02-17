/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;

/**
 * SQLScriptPosition
*
* @author Serge Rieder
*/
public class SQLScriptPosition extends Position implements Comparable<SQLScriptPosition> {

    private final ProjectionAnnotation foldingAnnotation;

    public SQLScriptPosition(int offset, int length, ProjectionAnnotation foldingAnnotation)
    {
        super(offset, length);
        this.foldingAnnotation = foldingAnnotation;
    }

    public ProjectionAnnotation getFoldingAnnotation()
    {
        return foldingAnnotation;
    }

    @Override
    public int compareTo(SQLScriptPosition o)
    {
        return offset - o.offset;
    }
}
