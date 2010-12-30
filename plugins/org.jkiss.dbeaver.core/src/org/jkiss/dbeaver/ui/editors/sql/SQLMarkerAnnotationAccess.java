package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccess;

/**
 * SQLMarkerAnnotationAccess
 */
public class SQLMarkerAnnotationAccess implements IAnnotationAccess {

    public Object getType(Annotation annotation) {
        return annotation.getType();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated assumed to always return <code>true</code>
     */
    public boolean isMultiLine(Annotation annotation) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated assumed to always return <code>true</code>
     */
    public boolean isTemporary(Annotation annotation) {
        return !annotation.isPersistent();
    }

}
