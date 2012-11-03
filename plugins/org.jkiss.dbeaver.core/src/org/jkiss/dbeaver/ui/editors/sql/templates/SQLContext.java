package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jdt.internal.corext.template.java.JavaVariable;
import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.*;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL context
 */
public class SQLContext extends DocumentTemplateContext implements IDataSourceProvider {

    private SQLEditorBase editor;
    private Map<String, SQLVariable> fVariables= new HashMap<String, SQLVariable>();

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

    @Override
    public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException
    {
        if (!canEvaluate(template))
            return null;

        TemplateTranslator translator= new TemplateTranslator() {
            @Override
            protected TemplateVariable createVariable(TemplateVariableType type, String name, int[] offsets) {
                SQLVariable variable= new SQLVariable(type, name, offsets);
                fVariables.put(name, variable);
                return variable;
            }
        };
        TemplateBuffer buffer = translator.translate(template);

        getContextType().resolve(buffer, this);

        return buffer;
    }

    TemplateVariable getTemplateVariable(String name) {
        TemplateVariable variable= fVariables.get(name);
        if (variable != null && !variable.isResolved())
            getContextType().resolve(variable, this);
        return variable;
    }

}
