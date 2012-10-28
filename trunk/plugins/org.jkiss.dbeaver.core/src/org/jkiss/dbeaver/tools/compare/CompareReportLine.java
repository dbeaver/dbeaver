package org.jkiss.dbeaver.tools.compare;

import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;

import java.util.List;

/**
* Report line
*/
class CompareReportLine {
    DBNDatabaseNode structure;
    DBNDatabaseNode[] nodes;
    List<CompareReportProperty> properties;
    int depth;
    boolean hasDifference;
}
