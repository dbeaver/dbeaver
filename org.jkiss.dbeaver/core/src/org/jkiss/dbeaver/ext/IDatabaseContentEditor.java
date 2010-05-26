package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorPart;

/**
 * Database content editor
 */
public interface IDatabaseContentEditor extends IEditorPart {

    boolean isContentValid();
    
}
