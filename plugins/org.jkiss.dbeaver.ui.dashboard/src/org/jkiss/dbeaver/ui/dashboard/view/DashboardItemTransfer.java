/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;

import java.util.List;

public final class DashboardItemTransfer extends LocalObjectTransfer<List<Object>> {

    public static final DashboardItemTransfer INSTANCE = new DashboardItemTransfer();
    private static final String TYPE_NAME = "DashboardTransfer.Item Transfer" + System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
    private static final int TYPEID = registerType(TYPE_NAME);

    private DashboardItemTransfer() {
    }

    @Override
    protected int[] getTypeIds() {
        return new int[]{TYPEID};
    }

    @Override
    protected String[] getTypeNames() {
        return new String[]{TYPE_NAME};
    }

}
