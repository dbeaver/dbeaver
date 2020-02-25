/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueDefaultGenerator;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Date;

/**
 * Date/time value handler
 */
public abstract class DateTimeValueHandler extends BaseValueHandler implements DBDValueDefaultGenerator {

    protected static final Log log = Log.getLog(DateTimeValueHandler.class);

    @NotNull
    @Override
    public Class<Date> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Date.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Date) {
            return (copy ? ((Date)object).clone() : object);
        } else {
            return object;
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        return super.getValueDisplayString(column, value, format);
    }

    ////////////////////////////////////////////////////////
    // Default generator

    @Override
    public String getDefaultValueLabel() {
        return "Current Time";
    }

    @Override
    public Object generateDefaultValue(DBCSession session, DBSTypedObject type) {
        try {
            return getValueFromObject(session, type, new Date(), false, false);
        } catch (DBCException e) {
            log.debug("Error getting current time stamp", e);
            return null;
        }
    }

}