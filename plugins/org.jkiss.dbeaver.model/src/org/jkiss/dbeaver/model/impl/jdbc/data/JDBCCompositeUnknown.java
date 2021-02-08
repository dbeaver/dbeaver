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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.Arrays;

/**
 * Unknown struct.
 */
public class JDBCCompositeUnknown extends JDBCComposite {

    public JDBCCompositeUnknown(@NotNull JDBCComposite struct, @NotNull DBRProgressMonitor monitor) throws DBCException {
        super(struct, monitor);
    }

    public JDBCCompositeUnknown(@NotNull DBCSession session, @Nullable Object structData) {
        this.type = new StructType(session.getDataSource());
        this.attributes = new DBSEntityAttribute[0];// { new StructAttribute(type, 0, structData) };
        this.values = new Object[]{structData};
    }

    @Override
    public JDBCCompositeUnknown cloneValue(DBRProgressMonitor monitor) throws DBCException {
        return new JDBCCompositeUnknown(this, monitor);
    }

    public String getStringRepresentation() {
        return Arrays.toString(values);
    }

}
