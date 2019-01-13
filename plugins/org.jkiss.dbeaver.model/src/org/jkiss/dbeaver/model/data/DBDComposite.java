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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Structured data record.
 * Consists of primitive values or other records
 */
public interface DBDComposite extends DBDComplexValue {

    DBSEntityAttribute[] EMPTY_ATTRIBUTE = new DBSEntityAttribute[0];
    Object[] EMPTY_VALUES = new Object[0];

    DBSDataType getDataType();

    @NotNull
    DBSAttributeBase[] getAttributes();

    @Nullable
    Object getAttributeValue(@NotNull DBSAttributeBase attribute)
        throws DBCException;

    void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value);

}
