package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.editors.text.TextEditor;

/**
 * LOB text editor
 */
public class LOBTextEditor extends TextEditor {

    public LOBTextEditor() {
        setDocumentProvider(new LOBDocumentProvider());
    }

}
