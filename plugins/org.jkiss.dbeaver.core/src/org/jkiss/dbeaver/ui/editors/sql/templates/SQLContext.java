/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.*;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * SQL context
 */
public class SQLContext extends DocumentTemplateContext implements DBPContextProvider {

    private SQLEditorBase editor;
    private Map<String, SQLVariable> variables = new HashMap<>();

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
    public DBCExecutionContext getExecutionContext()
    {
        return editor.getExecutionContext();
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
