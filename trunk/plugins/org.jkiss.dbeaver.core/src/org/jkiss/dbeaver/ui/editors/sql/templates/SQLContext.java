package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

/**
 * SQL context
 */
public class SQLContext extends DocumentTemplateContext {

    public SQLContext(TemplateContextType type, IDocument document, int offset, int length)
    {
        super(type, document, offset, length);
    }

    public SQLContext(TemplateContextType type, IDocument document, Position position, SQLEditorBase editor)
    {
        super(type, document, position);
    }

}
