package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;


public class XMLEditor extends BaseTextEditor {

    public XMLEditor()
    {
        setDocumentProvider(new FileRefDocumentProvider());

		configureInsertMode(ITextEditorExtension3.SMART_INSERT, false);
		setSourceViewerConfiguration(new XMLConfiguration());
		setDocumentProvider(new XMLDocumentProvider());
	}
	
	@Override
    public void dispose() {
		super.dispose();
	}
}
