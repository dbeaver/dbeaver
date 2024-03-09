package org.jkiss.dbeaver.ui.editors.sql.semantics.completion;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.analyzer.TableReferencesAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;

public class SQLQueryCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    public SQLQueryCompletionAnalyzer(SQLCompletionRequest request) {
        
    }

    @Override
    public void run(DBRProgressMonitor param) throws InvocationTargetException, InterruptedException {
        // TODO Auto-generated method stub
        
    }

    public List<SQLCompletionProposalBase> getProposals() {
        // TODO Auto-generated method stub
        
        return Collections.emptyList();
    }
}