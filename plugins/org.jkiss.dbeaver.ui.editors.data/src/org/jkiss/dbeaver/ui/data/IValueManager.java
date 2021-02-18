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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPropertyManager;

/**
 * UI Value Manager.
 */
public interface IValueManager
{
    String GROUP_ACTIONS_ADDITIONAL = "actions_add";

    /**
     * Fills context menu for certain value
     *
     * @param manager context menu manager
     * @param controller value controller
     * @param activeEditor    active editor
     * @throws DBCException on error
     */
    void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller, @Nullable IValueEditor activeEditor)
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
