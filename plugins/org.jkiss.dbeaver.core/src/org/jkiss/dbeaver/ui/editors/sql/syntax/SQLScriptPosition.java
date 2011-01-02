/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public int compareTo(SQLScriptPosition o)
    {
        return offset - o.offset;
    }
}
