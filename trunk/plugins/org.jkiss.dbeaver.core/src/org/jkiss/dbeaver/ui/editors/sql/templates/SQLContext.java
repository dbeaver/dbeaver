package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

/**
 * SQL context
 */
public class SQLContext extends DocumentTemplateContext implements IDataSourceProvider {

    private SQLEditorBase editor;

    public SQLContext(TemplateContextType type, IDocument document, Position position, SQLEditorBase editor)
    {
        super(type, document, position);
        this.editor = editor;
    }

    public SQLEditorBase getEditor()
    {
        return editor;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return editor.getDataSource();
    }
}
