package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class SQLEditorHandlerExpandCollapseAllFoldings extends AbstractHandler {
    private static final String PREFIX = "command.org.jkiss.dbeaver.ui.editors.sql.";
    private static final String SUFFIX = "AllFoldings";
    private static final String EXPAND_COMMAND_ID = PREFIX + "Expand" + SUFFIX;
    private static final String COLLAPSE_COMMAND_ID = PREFIX + "Collapse" + SUFFIX;

    @Override
    public Object execute(ExecutionEvent event) {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor == null) {
            return null;
        }
        SQLEditorBase sqlEditor = activeEditor.getAdapter(SQLEditorBase.class);
        if (sqlEditor == null || !sqlEditor.isFoldingEnabled()) {
            return null;
        }
        ProjectionAnnotationModel model = sqlEditor.getAnnotationModel();
        if (model == null) {
            return null;
        }
        IDocument document = sqlEditor.getDocument();
        if (document == null) {
            return null;
        }
        int length = sqlEditor.getDocument().getLength();
        String commandId = event.getCommand().getId();
        if (EXPAND_COMMAND_ID.equals(commandId)) {
            model.expandAll(0, length);
        } else if (COLLAPSE_COMMAND_ID.equals(commandId)) {
            model.collapseAll(0, length);
        }
        return null;
    }
}
