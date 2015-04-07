/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

/**
 * IResultSetController
 */
public interface IResultSetController {

    public static final String MENU_GROUP_EDIT = "edit";

    @NotNull
    IWorkbenchPartSite getSite();

    @NotNull
    ResultSetModel getModel();

    @Nullable
    DBSDataContainer getDataContainer();

    @NotNull
    DBDDataReceiver getDataReceiver();

    @Nullable
    DBCExecutionContext getExecutionContext();

    boolean hasData();

    boolean isHasMoreData();

    boolean isReadOnly();

    boolean isRecordMode();

    boolean isAttributeReadOnly(DBDAttributeBinding attr);

    @NotNull
    IPreferenceStore getPreferenceStore();

    void applyChanges(@Nullable DBRProgressMonitor monitor);

    void rejectChanges();

    /**
     * Refreshes data. Reads data from underlying data container
     */
    void refreshData(@Nullable Runnable onSuccess);

    boolean isRefreshInProgress();

    /**
     * Reads next segment of data
     */
    void readNextSegment();

    /**
     * Redraws results and updates all toolbars/edit controls
     * @param rowsChanged updates contents
     */
    void redrawData(boolean rowsChanged);

    void fillContextMenu(@NotNull IMenuManager manager, @Nullable DBDAttributeBinding attr, @Nullable ResultSetRow row);

    @Nullable
    ResultSetRow getCurrentRow();

    void setCurrentRow(@Nullable ResultSetRow row);

    void navigateAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow row)
        throws DBException;

    void updateValueView();

    void updateEditControls();

    void fireResultSetChange();

    void setStatus(String message, boolean error);

    void updateStatusMessage();

    Color getDefaultBackground();

    Color getDefaultForeground();

    IResultSetPresentation getActivePresentation();
}
