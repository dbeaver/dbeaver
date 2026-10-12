/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.ui.tools;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class FireBirdEventToolCommandHandler extends AbstractHandler {

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        for (Object object : NavigatorUtils.getSelectedObjects(HandlerUtil.getCurrentSelection(event))) {
            FireBirdDataSource dataSource = null;
            if (object instanceof DBSObject) {
                DBPDataSource ds = ((DBSObject) object).getDataSource();
                if (ds instanceof FireBirdDataSource) {
                    dataSource = (FireBirdDataSource) ds;
                }
            } else if (object instanceof DBPDataSourceContainer) {
                DBPDataSource ds = ((DBPDataSourceContainer) object).getDataSource();
                if (ds instanceof FireBirdDataSource) {
                    dataSource = (FireBirdDataSource) ds;
                }
            }
            if (dataSource != null) {
                FireBirdEventDialog dialog = new FireBirdEventDialog(
                    HandlerUtil.getActiveWorkbenchWindow(event).getShell(),
                    dataSource
                );
                return dialog.open();
            }
        }
        return null;
    }
}
