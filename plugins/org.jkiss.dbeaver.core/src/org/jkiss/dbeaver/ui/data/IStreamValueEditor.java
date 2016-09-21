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

}
