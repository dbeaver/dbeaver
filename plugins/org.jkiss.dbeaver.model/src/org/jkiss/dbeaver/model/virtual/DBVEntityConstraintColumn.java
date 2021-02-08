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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;

/**
 * Constraint column
 */
public class DBVEntityConstraintColumn implements DBSEntityAttributeRef {

    private final DBVEntityConstraint constraint;
    private final String attributeName;

    public DBVEntityConstraintColumn(DBVEntityConstraint constraint, String attributeName)
    {
        this.constraint = constraint;
        this.attributeName = attributeName;
    }

    public DBVEntityConstraintColumn(DBVEntityConstraint constraint, DBVEntityConstraintColumn copy) {
        this.constraint = constraint;
        this.attributeName = copy.attributeName;
    }

    @NotNull
    @Override
    public DBSEntityAttribute getAttribute()
    {
        // Here we use void monitor.
        // In real life entity columns SHOULD be already read so it doesn't matter
        // But I'm afraid that in some very special cases it does. Thant's too bad.
        return constraint.getEntity().getAttribute(new VoidProgressMonitor(), attributeName);
    }

    public String getAttributeName()
    {
        return attributeName;
    }
}
