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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.dbeaver.ext.postgresql.model.data.PostgreStringValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueBinder;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * RedshiftStringValueHandler
 */
public class RedshiftStringValueHandler extends PostgreStringValueHandler implements DBDValueBinder {
    public static final RedshiftStringValueHandler INSTANCE = new RedshiftStringValueHandler();

    @Override
    public String makeQueryBind(DBSAttributeBase attribute, Object value) throws DBCException {
	return value == null ? null : "'" + value + "'";
    }
}
