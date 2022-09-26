/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.object.config;

import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.PropertyObjectEditPage;

import java.util.Map;

/**
 * Property object configurator
 */
public class PropertyObjectConfigurator implements DBEObjectConfigurator<DBSObject> {
    @Override
    public DBSObject configureObject(DBRProgressMonitor monitor, Object table, DBSObject object, Map<String, Object> options) {
        return UITask.run(() -> {
            final PropertyObjectEditPage page = new PropertyObjectEditPage(null, object);
            if (!page.edit()) {
                return null;
            }
            return object;
        });
    }

}
