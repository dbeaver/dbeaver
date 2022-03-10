/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.util.StringJoiner;

class ResultSetStatListener extends ResultSetListenerAdapter {
    private final ResultSetViewer viewer;
    private SLUpdateJob updateJob;

    ResultSetStatListener(ResultSetViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void handleResultSetSelectionChange(SelectionChangedEvent event) {
        IResultSetSelection selection = viewer.getSelection();
        IWorkbenchPartSite site = viewer.getSite();
        if (site instanceof IEditorSite) {
            // Use job with 100ms delay to avoid event spam
            if (this.updateJob == null) {
                this.updateJob = new SLUpdateJob();
            }
            this.updateJob.schedule(100);
        }
    }

    class SLUpdateJob extends UIJob {

        SLUpdateJob() {
            super("Update status line");
            setSystem(true);
            setUser(false);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            IResultSetSelection selection = viewer.getSelection();
            DBPPreferenceStore preferenceStore = viewer.getPreferenceStore();
            updateSelectionStatistics(selection, preferenceStore);
            return Status.OK_STATUS;
        }

        private void updateSelectionStatistics(IResultSetSelection selection, DBPPreferenceStore preferenceStore) {
            if (selection.getSelectedAttributes().isEmpty()) {
                updateSelectionStatistics(preferenceStore, null);
            } else if (selection instanceof IResultSetSelectionExt) {
                IResultSetSelectionExt selectionExt = (IResultSetSelectionExt) selection;
                updateSelectionStatistics(preferenceStore, selectionExt);
            }
        }

        private void updateSelectionStatistics(@NotNull DBPPreferenceStore preferenceStore, @Nullable IResultSetSelectionExt selectionExt) {
            StringJoiner slText = new StringJoiner(", ");
            if (preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_SEL_ROWS)) {
                slText.add("Rows: " + (selectionExt == null ? 0 : selectionExt.getSelectedRowCount())) ;// + "/" + selExt.getSelectedColumnCount() + "/" + selExt.getSelectedCellCount();
            }
            if (preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_SEL_COLUMNS)) {
                slText.add("Cols: " + (selectionExt == null ? 0 : selectionExt.getSelectedColumnCount()));
            }
            if (preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_SEL_CELLS)) {
                slText.add("Cells: " + (selectionExt == null ? 0 : selectionExt.getSelectedCellCount()));
            }
            viewer.setSelectionStatistics(slText.toString());
        }
    }

}
