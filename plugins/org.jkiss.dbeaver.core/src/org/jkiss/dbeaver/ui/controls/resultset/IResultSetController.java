/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

import java.util.List;

/**
 * ResultSet controller.
 * This interface is not supposed to be implemented by clients.
 */
public interface IResultSetController extends DBPContextProvider {

    String MENU_GROUP_EDIT = "edit";

    @NotNull
    IWorkbenchPartSite getSite();

    @NotNull
    IResultSetContainer getContainer();

    @NotNull
    ResultSetModel getModel();

    @Nullable
    DBSDataContainer getDataContainer();

    @NotNull
    DBDDataReceiver getDataReceiver();

    boolean hasData();

    boolean isHasMoreData();

    boolean isReadOnly();

    boolean isRecordMode();

    boolean isAttributeReadOnly(DBDAttributeBinding attr);

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    Color getDefaultBackground();

    @NotNull
    Color getDefaultForeground();

    boolean applyChanges(@Nullable DBRProgressMonitor monitor);

    void rejectChanges();

    List<DBEPersistAction> generateChangesScript(@NotNull DBRProgressMonitor monitor);

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

    void navigateAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow row, boolean newWindow)
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

    IResultSetPanel getVisiblePanel();

    IResultSetPanel[] getActivePanels();

    void activatePanel(String id, boolean setActive, boolean showPanels);

    void updatePanelActions();

    void updatePanelsContent(boolean forceRefresh);

    void setDataFilter(final DBDDataFilter dataFilter, boolean refreshData);

    /**
     * Enable/disable viewer actions. May be used by editors to "lock" RSV actions like navigation, edit, etc.
     * Actions will be locked until lockedBy will be disposed
     * @param lockedBy    locker control
     */
    void lockActionsByControl(Control lockedBy);

    void lockActionsByFocus(Control lockedBy);

    IResultSetSelection getSelection();
}
