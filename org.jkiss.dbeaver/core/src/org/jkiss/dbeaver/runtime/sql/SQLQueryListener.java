package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.ui.editors.sql.SQLScriptLine;

/**
 * SQLQueryListener
 */
public interface SQLQueryListener {

    void onStartJob();

    void onStartQuery(SQLScriptLine query);

    void onEndQuery(SQLQueryResult result);

    void onEndJob(boolean hasErrors);
}
