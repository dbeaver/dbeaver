/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.findandreplace.overlay.FindReplaceOverlay;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.exec.SQLNativeExecutorDescriptor;
import org.jkiss.dbeaver.ui.actions.exec.SQLNativeExecutorRegistry;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationDescriptor.QueryMode;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Set;

/**
 * SQLEditorPropertyTester
 */
public class SQLEditorPropertyTester extends PropertyTester {
    static final Log log = Log.getLog(SQLEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.sql";
    public static final String PROP_CAN_EXECUTE = "canExecute";
    public static final String PROP_CAN_EXECUTE_NATIVE = "canExecuteNative";
    public static final String PROP_CAN_EXPLAIN = "canExplain";
    public static final String PROP_CAN_NAVIGATE = "canNavigate";
    public static final String PROP_CAN_EXPORT = "canExport";
    public static final String PROP_HAS_ACTIVE_QUERY = "hasActiveQuery";
    public static final String PROP_HAS_SELECTION = "hasSelection";
    public static final String PROP_IS_ACTIVE_QUERY_RUNNING = "isActiveQueryRunning";
    public static final String PROP_FOLDING_SUPPORTED = "foldingSupported";
    public static final String PROP_FOLDING_ENABLED = "foldingEnabled";

    private static final String OVERLAY_ID_DATA_KEY = FindReplaceOverlay.ID_DATA_KEY;
    private static final Set<String> OVERLAY_ID_INPUTS = Set.of("replaceInput", "searchInput");

    public SQLEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof SQLEditor editor)) {
            return false;
        }
        final Control editorControl = editor.getEditorControl();
        if (editorControl == null) {
            return false;
        }
        boolean hasConnection = editor.getDataSourceContainer() != null;
        switch (property) {
            case PROP_CAN_EXECUTE: {
                var descriptor = editor.getActivePresentationDescriptor();
                var mode = descriptor != null ? descriptor.getQueryMode() : QueryMode.MULTIPLE;
                return switch (CommonUtils.toString(expectedValue)) {
                    case "statement" -> mode != QueryMode.NONE && isFocusNotInFindReplaceOverlay();
                    case "script" -> mode == QueryMode.MULTIPLE;
                    default -> false;
                };
            }
            case PROP_CAN_EXECUTE_NATIVE: {
                try {
                    if (editor.getDataSourceContainer() == null) {
                        return false;
                    }
                    SQLNativeExecutorDescriptor executorDescriptor =
                        SQLNativeExecutorRegistry.getInstance().getExecutorDescriptor(editor.getDataSourceContainer());
                    return executorDescriptor != null && executorDescriptor.getNativeExecutor() != null;
                } catch (DBException exception) {
                    log.error("Error checking native execution", exception);
                    return false;
                }
            }
            case PROP_CAN_EXPLAIN:
                return hasConnection && GeneralUtils.adapt(editor.getDataSource(), DBCQueryPlanner.class) != null;
            case PROP_CAN_NAVIGATE: {
                // Check whether some word is under cursor
                ISelectionProvider selectionProvider = editor.getSelectionProvider();
                if (selectionProvider == null) {
                    return false;
                }
                ITextSelection selection = (ITextSelection) selectionProvider.getSelection();
                IDocument document = editor.getDocument();
                return selection != null && document != null && !new SQLIdentifierDetector(
                    editor.getSyntaxManager().getDialect(),
                    editor.getSyntaxManager().getStructSeparator(),
                    editor.getSyntaxManager().getIdentifierQuoteStrings()
                ).extractIdentifier(document, new Region(selection.getOffset(), selection.getLength()), editor.getRuleManager()).isEmpty();
            }
            case PROP_CAN_EXPORT:
                return hasConnection && editor.hasActiveQuery();
            case PROP_HAS_ACTIVE_QUERY:
                return editor.hasActiveQuery();
            case PROP_HAS_SELECTION: {
                ISelection selection = editor.getSelectionProvider().getSelection();
                return selection instanceof ITextSelection && ((ITextSelection) selection).getLength() > 0;
            }
            case PROP_IS_ACTIVE_QUERY_RUNNING:
                return editor.isActiveQueryRunning();
            case PROP_FOLDING_ENABLED:
                return editor.isFoldingEnabled();
            case PROP_FOLDING_SUPPORTED:
                return editor.getProjectionAnnotationModel() != null;
        }
        return false;
    }

    private static boolean isFocusNotInFindReplaceOverlay() {
        Display display = UIUtils.getDisplay();
        Control focus = display.getFocusControl();
        if (focus != null && !focus.isDisposed() && focus.getParent() != null && !focus.getParent().isDisposed()) {
            Object id = focus.getParent().getData(OVERLAY_ID_DATA_KEY);
            return !(id instanceof String sid && OVERLAY_ID_INPUTS.contains(sid));
        }
        return true;
    }

    public static void firePropertyChange(String propName) {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }
}