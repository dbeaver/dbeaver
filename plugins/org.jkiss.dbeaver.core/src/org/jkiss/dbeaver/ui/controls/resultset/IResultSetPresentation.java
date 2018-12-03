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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;

/**
 * Result set renderer.
 * Visualizes result set viewer/editor.
 *
 * May additionally implement IResultSetEditor, ISelectionProvider, IStatefulControl
 */
public interface IResultSetPresentation {

    enum PresentationType {
        COLUMNS(true),
        DOCUMENT(true),
        CUSTOM(true),
        TRANSFORMER(false);

        /**
         * Persistent presentation will be reused next time user will show the same resultset.
         */
        public boolean isPersistent() {
            return persistent;
        }

        private final boolean persistent;

        PresentationType(boolean persistent) {
            this.persistent = persistent;
        }
    }

    enum RowPosition {
        FIRST,
        PREVIOUS,
        NEXT,
        LAST,
        CURRENT
    }

    void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent);

    IResultSetController getController();

    Control getControl();

    /**
     * Refreshes data
     * @param refreshMetadata    true if contents structure should be reloaded
     * @param append             appends data
     * @param keepState          commands to keep current presentation state even if refreshMetadata is true (usually this means data refresh/reorder).
     */
    void refreshData(boolean refreshMetadata, boolean append, boolean keepState);

    /**
     * Called after results refresh
     * @param refreshData data was refreshed
     */
    void formatData(boolean refreshData);

    void clearMetaData();

    void updateValueView();

    boolean isDirty();

    void applyChanges();

    /**
     * Called by controller to fill context menu.
     * Note: context menu invocation must be initiated by presentation, then it should call controller's
     * {@link org.jkiss.dbeaver.ui.controls.resultset.IResultSetController#fillContextMenu} which then will
     * call this function.
     * Cool, huh?
     * @param menu    menu
     */
    void fillMenu(@NotNull IMenuManager menu);

    void changeMode(boolean recordMode);

    void scrollToRow(@NotNull RowPosition position);

    @Nullable
    DBDAttributeBinding getCurrentAttribute();

    void setCurrentAttribute(@NotNull DBDAttributeBinding attribute);

    @Nullable
    Point getCursorLocation();

    @Nullable
    String copySelectionToString(ResultSetCopySettings settings);

    void printResultSet();

}
