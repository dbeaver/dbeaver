package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.jkiss.code.Nullable;

class MutableSQLScriptPosition extends Position {
    @Nullable
    private ProjectionAnnotation annotation;

    private boolean isMultiline;

    MutableSQLScriptPosition(int offset, int length) {
        super(offset, length);
    }

    @Nullable
    public ProjectionAnnotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(@Nullable ProjectionAnnotation annotation) {
        this.annotation = annotation;
    }

    public boolean isMultiline() {
        return isMultiline;
    }

    public void setMultiline(boolean multiline) {
        isMultiline = multiline;
    }
}
