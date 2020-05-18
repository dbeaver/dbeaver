/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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

    public SQLContext(TemplateContextType type, IDocument document, Position position, SQLEditorBase editor) {
        super(type, document, position);
        this.editor = editor;
    }

    public SQLEditorBase getEditor() {
        return editor;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return editor.getExecutionContext();
    }

    @Override
    public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
        if (!canEvaluate(template))
            return null;

        TemplateTranslator translator = new TemplateTranslator() {
            @Override
            protected TemplateVariable createVariable(TemplateVariableType type, String name, int[] offsets) {
                SQLVariable variable = new SQLVariable(SQLContext.this, type, name, offsets);
                variables.put(name, variable);
                return variable;
            }
        };
        TemplateBuffer buffer = translator.translate(template);
        formatTemplate(buffer);
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

    private void formatTemplate(TemplateBuffer buffer) {
        TemplateVariable[] variables = buffer.getVariables();
        final String indentation = getIndentation();
        String content = buffer.getString();
        if (!indentation.isEmpty() && content.indexOf('\n') != -1) {
            StringBuilder result = new StringBuilder();

            //how many places
            int nCountPlace = 0;
            for (TemplateVariable variable : variables) {
                nCountPlace = nCountPlace + variable.getOffsets().length;
            }

            //fill array
            int[][] aVarOffset = new int[nCountPlace][3];
            nCountPlace = 0;
            for (int i = 0; i < variables.length; i++) {
                int[] aOffsets = variables[i].getOffsets();
                for (int aOffset : aOffsets) {
                    aVarOffset[nCountPlace][0] = aOffset;
                    aVarOffset[nCountPlace][1] = i;
                    aVarOffset[nCountPlace][2] = aOffset;
                    nCountPlace = nCountPlace + 1;
                }
            }

            //sort
            for (int i = 0; i < nCountPlace - 1; i++) {
                for (int j = i + 1; j < nCountPlace; j++) {
                    if (aVarOffset[i][0] > aVarOffset[j][0]) {
                        int[] tmp = aVarOffset[i];
                        aVarOffset[i] = aVarOffset[j];
                        aVarOffset[j] = tmp;
                    }
                }
            }

            int iPlace = 0;
            int iSpaceLen = indentation.length();

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                result.append(c);
                if (c == '\n') {
                    //skip
                    while (iPlace < nCountPlace && i > aVarOffset[iPlace][0]) iPlace++;
                    //move
                    for (int j = iPlace; j < nCountPlace; j++) aVarOffset[j][2] = aVarOffset[j][2] + iSpaceLen;
                    result.append(indentation);
                }
            }

            //collect back
            for (int i = 0; i < variables.length; i++) {
                int[] aOffsets = variables[i].getOffsets();
                int iInd = 0;
                for (int j = 0; j < nCountPlace; j++) {
                    if (aVarOffset[j][1] == i) {
                        aOffsets[iInd] = aVarOffset[j][2];
                        iInd++;
                    }
                }
            }

            buffer.setContent(result.toString(), variables);
        }
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

    SQLVariable getTemplateVariable(String name) {
        SQLVariable variable = variables.get(name);
        if (variable != null && !variable.isResolved())
            getContextType().resolve(variable, this);
        return variable;
    }

    Collection<SQLVariable> getVariables() {
        return variables.values();
    }

    private String getIndentation() {
        int start = this.getStart();
        IDocument document = this.getDocument();

        try {
            IRegion region = document.getLineInformationOfOffset(start);
            int lineIndent = start - region.getOffset();
            if (lineIndent <= 0) {
                return "";
            }
            char[] buf = new char[lineIndent];
            for (int i = 0; i < lineIndent; i++) {
                buf[i] = ' ';
            }
            return String.valueOf(buf);
        } catch (Exception var6) {
            return "";
        }
    }
}
