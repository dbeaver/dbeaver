package org.jkiss.dbeaver.ui.controls.resultset;

import java.util.List;

public class ResultSetDataContainerOptions {

    private boolean exportSelectedRows;
    private List<Long> selectedRows;
    private boolean exportSelectedColumns;
    private List<String> selectedColumns;

    public boolean isExportSelectedRows() {
        return exportSelectedRows;
    }

    public void setExportSelectedRows(boolean exportSelectedRows) {
        this.exportSelectedRows = exportSelectedRows;
    }

    public List<Long> getSelectedRows() {
        return selectedRows;
    }

    public void setSelectedRows(List<Long> selectedRows) {
        this.selectedRows = selectedRows;
    }

    public boolean isExportSelectedColumns() {
        return exportSelectedColumns;
    }

    public void setExportSelectedColumns(boolean exportSelectedColumns) {
        this.exportSelectedColumns = exportSelectedColumns;
    }

    public List<String> getSelectedColumns() {
        return selectedColumns;
    }

    public void setSelectedColumns(List<String> selectedColumns) {
        this.selectedColumns = selectedColumns;
    }
}
