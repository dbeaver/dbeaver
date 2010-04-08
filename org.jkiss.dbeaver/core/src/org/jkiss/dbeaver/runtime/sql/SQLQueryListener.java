package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.ui.editors.sql.SQLStatementInfo;

/**
 * SQLQueryListener
 */
public interface SQLQueryListener {

    void onStartJob();

    void onStartQuery(SQLStatementInfo query);

    void onEndQuery(SQLQueryResult result);

    void onEndJob(boolean hasErrors);
}
