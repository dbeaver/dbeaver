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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.ext.altibase.model.AltibaseTablespace.State;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.ByteNumberFormat;

import java.math.BigInteger;
import java.sql.ResultSet;

public class AltibaseDataFile4Disk extends AltibaseDataFile {

    private BigInteger currSize;
    private BigInteger nextSize;
    private BigInteger initSize;
    private BigInteger maxSize;
    private boolean isAutoExtend;
    private State state;
    
    protected AltibaseDataFile4Disk(AltibaseTablespace tablespace, ResultSet dbResult) {
        super(tablespace, dbResult);
        this.currSize = new BigInteger(JDBCUtils.safeGetString(dbResult, "CURRSIZE"));
        this.nextSize = new BigInteger(JDBCUtils.safeGetString(dbResult, "NEXTSIZE"));
        this.initSize = new BigInteger(JDBCUtils.safeGetString(dbResult, "INITSIZE"));
        this.maxSize  = new BigInteger(JDBCUtils.safeGetString(dbResult, "MAXSIZE"));
        this.isAutoExtend = (JDBCUtils.safeGetInt(dbResult, "AUTOEXTEND") == 1);
        this.state = State.getStateByIdx(JDBCUtils.safeGetInt(dbResult, "STATE"));
    }

    
    @Property(viewable = true, order = 4, formatter = ByteNumberFormat.class)
    public BigInteger getCurrSize() {
        return currSize.multiply(new BigInteger(this.getParentObject().getPageSizeInBytesStr()));
    }
    
    @Property(viewable = true, order = 5, formatter = ByteNumberFormat.class)
    public BigInteger getNextSize() {
        return nextSize.multiply(new BigInteger(this.getParentObject().getPageSizeInBytesStr()));
    }
    
    @Property(viewable = true, order = 6, formatter = ByteNumberFormat.class)
    public BigInteger getInitSize() {
        return initSize.multiply(new BigInteger(this.getParentObject().getPageSizeInBytesStr()));
    }
    
    @Property(viewable = true, order = 7, formatter = ByteNumberFormat.class)
    public BigInteger getMaxSize() {
        return maxSize.multiply(new BigInteger(this.getParentObject().getPageSizeInBytesStr()));
    }
    
    @Property(viewable = true, order = 8)
    public boolean getAutoExtend() {
        return this.isAutoExtend;
    }
    
    @Property(viewable = true, order = 10)
    public String getState() {
        return this.state.name();
    }
}