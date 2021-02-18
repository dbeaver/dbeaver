/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Stream Value Editor.
 * Wrapped in base value editor.
 */
public interface IStreamValueEditor<CONTROL extends Control>
{
    /**
     * Gets control which actually performs edit
     * @return control reference
     * @param valueController    value controller
     */
    CONTROL createControl(IValueController valueController);

    /**
     * Extracts value from value editor.
     * @throws DBException on any error
     */
    void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull CONTROL control, @NotNull DBDContent value)
        throws DBException;

    /**
     * Fills current editor with specified value. Do not updates value in controller.
     * @param value new value for editor
     * @throws DBException on any error
     */
    void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull CONTROL control, @NotNull DBDContent value)
        throws DBException;

    /**
     * Fills menu/toolbar with extra actions
     *
     * @param manager context menu manager
     * @throws DBCException on error
     */
    void contributeActions(@NotNull IContributionManager manager, @NotNull CONTROL control)
        throws DBCException;

    void contributeSettings(@NotNull IContributionManager manager, @NotNull CONTROL control)
            throws DBCException;

}
