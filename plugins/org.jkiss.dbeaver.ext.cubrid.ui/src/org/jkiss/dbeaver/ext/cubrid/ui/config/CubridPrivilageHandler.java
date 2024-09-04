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
package org.jkiss.dbeaver.ext.cubrid.ui.config;

import org.jkiss.dbeaver.ext.cubrid.model.CubridPrivilage;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;

public enum CubridPrivilageHandler implements DBEPropertyHandler<CubridPrivilage>, DBEPropertyReflector<CubridPrivilage>
{
    NAME,
    PASSWORD,
    DESCRIPTION, GROUPS;

    @Override
    public void reflectValueChange(CubridPrivilage object, Object oldValue, Object newValue) {
        // TODO Auto-generated method stub
        if (this == NAME) {
            object.setName(newValue.toString());
        }
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return name();
    }

    @Override
    public DBECommandComposite createCompositeCommand(CubridPrivilage object) {
        // TODO Auto-generated method stub
        return new CubridCommandHandler(object);
    }


}