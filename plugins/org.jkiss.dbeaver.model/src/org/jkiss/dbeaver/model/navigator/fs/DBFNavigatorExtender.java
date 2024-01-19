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
package org.jkiss.dbeaver.model.navigator.fs;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNModelExtender;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

public class DBFNavigatorExtender implements DBNModelExtender {
    @Nullable
    @Override
    public DBNNode[] getExtraNodes(@NotNull DBNNode parentNode) {
        if (parentNode instanceof DBNProject) {
            if (ArrayUtils.isEmpty(DBWorkbench.getPlatform().getFileSystemRegistry().getFileSystemProviders())) {
                return null;
            }
            return new DBNNode[]{new DBNFileSystems((DBNProject) parentNode)};
        }
        return null;
    }
}
