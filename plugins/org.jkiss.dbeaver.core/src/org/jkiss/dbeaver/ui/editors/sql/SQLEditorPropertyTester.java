/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.parser.SQLIdentifierDetector;

/**
 * SQLEditorPropertyTester
 */
public class SQLEditorPropertyTester extends PropertyTester
{
    //static final Log log = Log.getLog(SQLEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.sql";
    public static final String PROP_CAN_EXECUTE = "canExecute";
    public static final String PROP_CAN_EXPLAIN = "canExplain";
    public static final String PROP_CAN_NAVIGATE = "canNavigate";
    public static final String PROP_CAN_EXPORT = "canExport";
    public static final String PROP_HAS_SELECTION = "hasSelection";

    public SQLEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof SQLEditorBase)) {
            return false;
        }
        SQLEditor editor = (SQLEditor)receiver;
        final Control editorControl = editor.getEditorControl();
        if (editorControl == null) {
            return false;
        }
        boolean hasConnection = editor.getDataSourceContainer() != null;
        switch (property) {
            case PROP_CAN_EXECUTE:
                // Do not check hasActiveQuery - sometimes jface don't update action enablement after cursor change/typing
                return true;/* && (!"statement".equals(expectedValue) || editor.hasActiveQuery())*/
            case PROP_CAN_EXPLAIN:
                return hasConnection && DBUtils.getAdapter(DBCQueryPlanner.class, editor.getDataSource()) != null;
            case PROP_CAN_NAVIGATE: {
                // Check whether some word is under cursor
                ISelectionProvider selectionProvider = editor.getSelectionProvider();
                if (selectionProvider == null) {
                    return false;
                }
                ITextSelection selection = (ITextSelection) selectionProvider.getSelection();
                Document document = editor.getDocument();
                return
                    selection != null &&
                        document != null &&
                        !new SQLIdentifierDetector(
                            editor.getSyntaxManager().getStructSeparator(),
                            editor.getSyntaxManager().getQuoteStrings())
                            .detectIdentifier(document, new Region(selection.getOffset(), selection.getLength())).isEmpty();
            }
            case PROP_CAN_EXPORT:
                return hasConnection && editor.hasActiveQuery();
            case PROP_HAS_SELECTION: {
                ISelection selection = editor.getSelectionProvider().getSelection();
                return selection instanceof ITextSelection && ((ITextSelection) selection).getLength() > 0;
            }
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}