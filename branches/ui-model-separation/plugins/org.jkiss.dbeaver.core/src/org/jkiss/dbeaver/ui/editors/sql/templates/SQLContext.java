package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.*;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * SQL context
 */
public class SQLContext extends DocumentTemplateContext implements IDataSourceProvider {

    private SQLEditorBase editor;
    private Map<String, SQLVariable> variables = new HashMap<String, SQLVariable>();

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

        TemplateTranslator translator = new TemplateTranslator() {
            @Override
            protected TemplateVariable createVariable(TemplateVariableType type, String name, int[] offsets)
            {
                SQLVariable variable = new SQLVariable(SQLContext.this, type, name, offsets);
                variables.put(name, variable);
                return variable;
            }
        };
        TemplateBuffer buffer = translator.translate(template);
/*
        // Reorder variables
        TemplateVariable[] bufferVariables = buffer.getVariables();

        Arrays.sort(bufferVariables, new Comparator<TemplateVariable>() {
            @Override
            public int compare(TemplateVariable o1, TemplateVariable o2)
            {
                return variableOrder(o1.getName()) - variableOrder(o2.getName());
            }
        });
        buffer = new TemplateBuffer(buffer.getString(), bufferVariables);
*/

        getContextType().resolve(buffer, this);

        return buffer;
    }

/*
    private static final String[] VAR_ORDER = {
        "table",
        "column",
        "value"
    };

    private static int variableOrder(String name)
    {
        for (int i = 0; i < VAR_ORDER.length; i++) {
            if (name.equals(VAR_ORDER[i])) {
                return i;
            }
        }
        return VAR_ORDER.length + 1;
    }
*/

    SQLVariable getTemplateVariable(String name)
    {
        SQLVariable variable = variables.get(name);
        if (variable != null && !variable.isResolved())
            getContextType().resolve(variable, this);
        return variable;
    }

    Collection<SQLVariable> getVariables()
    {
        return variables.values();
    }
}
