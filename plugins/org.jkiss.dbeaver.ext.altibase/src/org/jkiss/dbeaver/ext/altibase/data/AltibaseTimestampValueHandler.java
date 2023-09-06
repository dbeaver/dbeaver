/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCObjectValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/* 
 * Altibase Timestamp constraint works like a data type, though it's not a real data type.
 */
public class AltibaseTimestampValueHandler extends JDBCObjectValueHandler {

    public static final AltibaseTimestampValueHandler INSTANCE = new AltibaseTimestampValueHandler();

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, 
            Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object != null && object instanceof JDBCContentBytes) {
            byte[] bytes = ((JDBCContentBytes) object).getRawValue();
            return BinaryFormatterHex.INSTANCE.toString(bytes, 0, bytes.length);
        }
        
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }
}
