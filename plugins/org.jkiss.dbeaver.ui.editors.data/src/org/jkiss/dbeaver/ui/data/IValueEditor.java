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

package org.jkiss.dbeaver.ui.data;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;

/**
 * DBD Value Editor.
 * Must be implemented by all visual value editors (dialogs, editors, inline controls).
 */
public interface IValueEditor
{
    /**
     * Create editor control(s)
     */
    void createControl();

    /**
     * Gets control which actually performs edit
     * @return control reference
     */
    Control getControl();

    /**
     * Extracts value from value editor.
     * @return value. Possibly NULL
     * @throws DBException on any error
     */
    @Nullable
    Object extractEditorValue()
        throws DBException;

    /**
     * Fills current editor with specified value.
     * In inline mode implementation should select whole value.
     * Do not updates value in controller.
     * @param value new value for editor
     * @throws DBException on any error
     */
    void primeEditorValue(@Nullable Object value)
        throws DBException;

    /**
     * Checks whether editor content was modified (after #primeEditorValue invocation).
     */
    boolean isDirty();

    void setDirty(boolean dirty);

    /**
     * Fills menu/toolbar with extra actions
     *
     * @param manager context menu manager
     * @param controller value controller
     * @throws DBCException on error
     */
    void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller)
        throws DBCException;

}
