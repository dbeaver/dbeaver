package org.jkiss.dbeaver.tools.compare;

import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;

import java.util.List;

/**
* Report
*/
class CompareReport {
    private List<DBNDatabaseNode> nodes;
    private List<CompareReportLine> reportLines;

    CompareReport(List<DBNDatabaseNode> nodes, List<CompareReportLine> reportLines)
    {
        this.nodes = nodes;
        this.reportLines = reportLines;
    }

    public List<DBNDatabaseNode> getNodes()
    {
        return nodes;
    }

    public List<CompareReportLine> getReportLines()
    {
        return reportLines;
    }
}
