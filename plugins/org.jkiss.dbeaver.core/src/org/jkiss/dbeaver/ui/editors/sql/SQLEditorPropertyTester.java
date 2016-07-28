/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLIdentifierDetector;

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
        //boolean isFocused = editorControl.isFocusControl();
        boolean hasConnection = editor.getDataSourceContainer() != null;
        switch (property) {
            case PROP_CAN_EXECUTE:
                // Do not check hasActiveQuery - sometimes jface don't update action enablement after cursor change/typing
                return hasConnection/* && (!"statement".equals(expectedValue) || editor.hasActiveQuery())*/;
            case PROP_CAN_EXPLAIN:
                return hasConnection && editor.hasActiveQuery() && DBUtils.getAdapter(DBCQueryPlanner.class, editor.getDataSource()) != null;
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
                            editor.getSyntaxManager().getQuoteSymbol())
                            .detectIdentifier(document, new Region(selection.getOffset(), selection.getLength())).isEmpty();
            }
            case PROP_CAN_EXPORT:
                return hasConnection && editor.hasActiveQuery();
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}