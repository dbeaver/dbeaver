/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
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
     * @param rowsChanged updates contents
     */
    void redrawData(boolean rowsChanged);

    void fillContextMenu(@NotNull IMenuManager manager, @Nullable DBDAttributeBinding attr, @Nullable ResultSetRow row);

    @Nullable
    ResultSetRow getCurrentRow();

    void setCurrentRow(@Nullable ResultSetRow row);

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


    /**
     * Enable/disable viewer actions. May be used by editors to "lock" RSV actions like navigation, edit, etc.
     * Actions will be locked until lockedBy will be disposed
     * @param lockedBy    locker control
     */
    void lockActionsByControl(Control lockedBy);

    void lockActionsByFocus(Control lockedBy);

}
