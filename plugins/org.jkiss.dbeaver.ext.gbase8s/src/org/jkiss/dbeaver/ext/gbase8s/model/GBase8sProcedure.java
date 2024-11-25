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

package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * @author Chao Tian
 */
public class GBase8sProcedure extends GenericProcedure {

    public GBase8sProcedure(GenericStructContainer container, String procedureName, String specificName,
            String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

    @Property(hidden = true, order = 3)
    @Override
    public GenericCatalog getCatalog() {
        return getContainer().getCatalog();
    }

    @Property(hidden = true, order = 5)
    @Override
    public GenericPackage getPackage() {
        return getContainer() instanceof GenericPackage ? (GenericPackage) getContainer() : null;
    }
}
