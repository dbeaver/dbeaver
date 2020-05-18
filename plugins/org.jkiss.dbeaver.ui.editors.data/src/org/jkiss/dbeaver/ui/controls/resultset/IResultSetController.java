/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.ui.data.IDataController;

import java.util.List;

/**
 * ResultSet controller.
 * This interface is not supposed to be implemented by clients.
 */
public interface IResultSetController extends IDataController, DBPContextProvider, DBPObject {

    String MENU_ID_EDIT = "edit";
    String MENU_ID_VIEW = "view";
    String MENU_ID_VIRTUAL_MODEL = "virtual_model";
    String MENU_ID_FILTERS = "filters";
    String MENU_ID_ORDER = "orderings";
    String MENU_ID_LAYOUT = "layout";
    String MENU_GROUP_EDIT = "edit";
    String MENU_GROUP_EXPORT = "results_export";
    String MENU_GROUP_ADDITIONS = "results_additions";//IWorkbenchActionConstants.MB_ADDITIONS;

    @NotNull
    IResultSetContainer getContainer();

    @NotNull
    IResultSetDecorator getDecorator();

    @NotNull
    IResultSetLabelProvider getLabelProvider();

    @NotNull
    ResultSetModel getModel();

    @NotNull
    DBDDataReceiver getDataReceiver();

    Composite getControl();

    boolean isReadOnly();

    boolean isRecordMode();

    String getReadOnlyStatus();

    String getAttributeReadOnlyStatus(DBDAttributeBinding attr);

    boolean isPanelsVisible();

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    Color getDefaultBackground();

    @NotNull
    Color getDefaultForeground();

    boolean applyChanges(@Nullable DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings);

    void rejectChanges();

    @Nullable
    ResultSetSaveReport generateChangesReport();

    List<DBEPersistAction> generateChangesScript(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings);
    
    void showDistinctFilter(DBDAttributeBinding curAttribute);

    void toggleSortOrder(DBDAttributeBinding columnElement, boolean forceAscending, boolean forceDescending);

    boolean checkForChanges();

    /**
     * Refreshes data. Reverts all changes and clears filters.
     */
    void refresh();

    /**
     * Refreshes data. Reads data from underlying data container
     */
    boolean refreshData(@Nullable Runnable onSuccess);

    boolean isRefreshInProgress();

    /**
     * Reads next segment of data
     */
    void readNextSegment();

    /**
     * Reads all rows from data container.
     * Note: in case of huge resultset this function may eventually throw {@link java.lang.OutOfMemoryError}
     */
    void readAllData();

    /**
     * Redraws results and updates all toolbars/edit controls
     * @param attributesChanged
     * @param rowsChanged updates contents
     */
    void redrawData(boolean attributesChanged, boolean rowsChanged);

    void fillContextMenu(@NotNull IMenuManager manager, @Nullable DBDAttributeBinding attr, @Nullable ResultSetRow row);

    @Nullable
    ResultSetRow getCurrentRow();

    void setCurrentRow(@Nullable ResultSetRow row);

    @NotNull
    ResultSetRow addNewRow(final boolean copyCurrent, boolean afterCurrent, boolean updatePresentation);

    ////////////////////////////////////////
    // Navigation & history

    /**
     * Navigates to association. One of @association OR @attr must be specified.
     */
    void navigateAssociation(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetModel model, @NotNull DBSEntityAssociation association, @NotNull List<ResultSetRow> rows, boolean newWindow)
        throws DBException;

    void navigateReference(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetModel bindingsModel, @NotNull DBSEntityAssociation association, @NotNull List<ResultSetRow> rows, boolean newWindow)
        throws DBException;

    int getHistoryPosition();

    int getHistorySize();

    void navigateHistory(int position);

    void setStatus(String message, DBPMessageType messageType);

    void updateStatusMessage();

    void updateEditControls();

    ////////////////////////////////////////
    // Presentation & panels

    /**
     * Active presentation
     */
    @NotNull
    IResultSetPresentation getActivePresentation();

    void showEmptyPresentation();

    IResultSetPanel getVisiblePanel();

    IResultSetPanel[] getActivePanels();

    boolean activatePanel(String id, boolean setActive, boolean showPanels);

    void updatePanelActions();

    void updatePanelsContent(boolean forceRefresh);

    void setDataFilter(final DBDDataFilter dataFilter, boolean refreshData);

    void setSegmentFetchSize(Integer segmentFetchSize);

    /**
     * Enable/disable viewer actions. May be used by editors to "lock" RSV actions like navigation, edit, etc.
     * Actions will be locked until lockedBy will be disposed
     * @param lockedBy    locker control
     */
    void lockActionsByControl(Control lockedBy);

    void lockActionsByFocus(Control lockedBy);

    IResultSetSelection getSelection();

    void addListener(IResultSetListener listener);

    void removeListener(IResultSetListener listener);
}
