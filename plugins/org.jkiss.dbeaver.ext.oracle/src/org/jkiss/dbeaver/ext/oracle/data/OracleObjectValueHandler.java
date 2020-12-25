/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;

/**
 * Object type support
 */
public class OracleObjectValueHandler extends JDBCStructValueHandler {

    public static final OracleObjectValueHandler INSTANCE = new OracleObjectValueHandler();

/*
    @NotNull
    @Override
    public Class<Object> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return java.lang.Object.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new OracleObjectValue(null);
        } else if (object instanceof Struct) {
            return super.getValueFromObject(session, type, object, copy);
        } else if (object instanceof OracleObjectValue) {
            return copy ? new OracleObjectValue(((OracleObjectValue) object).getValue()) : (OracleObjectValue)object;
        } else {
            return new OracleObjectValue(object);
        }
    }
*/

}
