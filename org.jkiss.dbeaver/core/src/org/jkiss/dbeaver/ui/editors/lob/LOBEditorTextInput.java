package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.core.resources.IFile;

/**
 * LOB editor infput for text content
 */
public class LOBEditorTextInput extends FileEditorInput {
    /**
     * Creates an editor input based of the given file resource.
     *
     * @param file the file resource
     */
    public LOBEditorTextInput(IFile file) {
        super(file);
    }
}
