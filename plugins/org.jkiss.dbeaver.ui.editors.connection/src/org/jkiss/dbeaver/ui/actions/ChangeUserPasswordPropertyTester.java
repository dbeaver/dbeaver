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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.access.DBAuthUtils;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.utils.CommonUtils;

public class ChangeUserPasswordPropertyTester extends PropertyTester {

    @Override
    public boolean test(@NotNull Object element, @NotNull String property, @NotNull Object[] objects, @Nullable Object expectedValue) {
        if (!(element instanceof DBNDataSource)) {
            return false;
        }

        if (property.equals("canChangePassword")) {
            DBNDataSource dsNode = (DBNDataSource) element;
            DBPDataSourceContainer dataSourceContainer = dsNode.getDataSourceContainer();
            if (!DBAuthUtils.canChangeCurrentUserPassword(dataSourceContainer)) {
                return false;
            }

            DBPDataSource dataSource = dataSourceContainer.getDataSource();
            String userName = DBAuthUtils.getCurrentUserName(dataSourceContainer);
            if (dataSource instanceof IAdaptable) {
                return ((IAdaptable) dataSource).getAdapter(DBAUserPasswordManager.class) != null && CommonUtils.isNotEmpty(userName);
            }
        }
        return false;
    }
}
