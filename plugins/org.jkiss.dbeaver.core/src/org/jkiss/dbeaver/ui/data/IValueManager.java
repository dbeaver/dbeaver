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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPPropertyManager;
import org.jkiss.dbeaver.model.exec.DBCException;

/**
 * UI Value Manager.
 */
public interface IValueManager
{
    /**
     * Fills context menu for certain value
     *
     * @param manager context menu manager
     * @param controller value controller
     * @throws DBCException on error
     */
    void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller)
        throws DBCException;

    /**
     * Fills value's custom properties
     * @param propertySource property source
     * @param controller value controller
     */
    void contributeProperties(@NotNull DBPPropertyManager propertySource, @NotNull IValueController controller);

    /**
     * Returns array of edit types supported by this value manager.
     */
    @NotNull
    IValueController.EditType[] getSupportedEditTypes();

    /**
     * Creates value editor.
     * Value editor could be:
     * <li>inline editor (control created withing inline placeholder)</li>
     * <li>dialog (modal or modeless)</li>
     * <li>workbench editor</li>
     * Modeless dialogs and editors must implement IValueEditor and
     * must register themselves within value controller. On close they must unregister themselves within
     * value controller.
     * @param controller value controller  @return true if editor was successfully opened.
     * makes since only for inline editors, otherwise return value is ignored.
     * @return true on success
     * @throws DBException on error
     */
    @Nullable
    IValueEditor createEditor(@NotNull IValueController controller)
        throws DBException;

}
