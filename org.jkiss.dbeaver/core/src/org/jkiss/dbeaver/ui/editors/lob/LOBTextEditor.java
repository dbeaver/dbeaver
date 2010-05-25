package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.editors.text.TextEditor;

/**
 * LOB text editor
 */
public class LOBTextEditor extends TextEditor {

    public LOBTextEditor() {
        setDocumentProvider(new LOBDocumentProvider());
    }

    @Override
    protected void updateStatusField(String category)
    {
        super.updateStatusField(
            category);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
