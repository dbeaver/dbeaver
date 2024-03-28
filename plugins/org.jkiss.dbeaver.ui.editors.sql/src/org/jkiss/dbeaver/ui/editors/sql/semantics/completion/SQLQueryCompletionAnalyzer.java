package org.jkiss.dbeaver.ui.editors.sql.semantics.completion;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLDocumentSyntaxContext;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLCompletionProposal;

public class SQLQueryCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);
    
    private final SQLEditorBase editor;
    private final SQLCompletionRequest request;
    
    private volatile List<SQLCompletionProposalBase> proposals = Collections.emptyList();

    public SQLQueryCompletionAnalyzer(SQLEditorBase editor, SQLCompletionRequest request) {
        this.editor = editor;
        this.request = request;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        SQLDocumentSyntaxContext syntaxContext = this.editor.getSyntaxContext();
        if (syntaxContext != null) {
            int position = this.request.getDocumentOffset();
            SQLQueryCompletionContext completionContext = syntaxContext.obtainCompletionContext(editor.getExecutionContext(), position);
            SQLQueryCompletionSet completionSet = completionContext.prepareProposal(monitor, position);
            
            this.proposals = new ArrayList<>(completionSet.getItems().size()); 
            for (SQLQueryCompletionItem item: completionSet.getItems()) {
                DBPNamedObject object = null;
                DBPImage image = item.getKind() == null ? DBValueFormatting.getObjectImage(item.getObject()) : switch (item.getKind()) {
                    case RESERVED -> UIIcon.SQL_TEXT;
                    case SUBQUERY_ALIAS -> DBIcon.TREE_TABLE_ALIAS;
                    case DERIVED_COLUMN_NAME -> DBIcon.TREE_FOREIGN_KEY_COLUMN;
                    case NEW_TABLE_NAME -> DBIcon.TREE_TABLE;
                    case USED_TABLE_NAME -> UIIcon.EDIT_TABLE;
                    case TABLE_COLUMN_NAME -> DBIcon.TREE_COLUMN;
                    default -> throw new IllegalStateException("Unexpected completion item kind " + item.getKind());
                };
                // TODO wtf resulting cursor position
                this.proposals.add(new SQLCompletionProposal(this.request, item.getText(), item.getText(), position, image, DBPKeywordType.OTHER, item.getDescription(), object, Collections.emptyMap()));
            }
        }
    }

    public List<SQLCompletionProposalBase> getProposals() {
        return this.proposals;
    }
}