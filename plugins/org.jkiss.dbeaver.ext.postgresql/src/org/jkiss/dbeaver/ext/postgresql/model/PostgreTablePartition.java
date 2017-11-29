/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *               2017 Andrew Khitrin   (andrew@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntity;

public class PostgreTablePartition extends PostgreTable
{
	
	public static final String CAT_PARTITIONING = "Partitioning";

	public PostgreTablePartition(PostgreSchema catalog) {
		super(catalog);
	}

	public PostgreTablePartition(PostgreSchema catalog, ResultSet dbResult) {
		super(catalog, dbResult);
	}
	
	
	
	 public PostgreTablePartition(PostgreSchema container, DBSEntity source, boolean persisted) {
		super(container, source, persisted);
	}

	@Property(category = CAT_PARTITIONING, editable = false, viewable = true, order = 50)
     public String getPartKeys() {
	        return "this.oid";
	    }

	
	/*
    private final PostgreTableBase partitionTable;
    private int sequenceNum;
//select * from  pg_partitioned_table where partrelid = ? 
    public PostgreTablePartition(
        @NotNull PostgreTableBase table,
        @NotNull PostgreTableBase partitionTable,
        int sequenceNum,
        boolean persisted)
    {
        super(table,
            table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "->" + partitionTable.getFullyQualifiedName(DBPEvaluationContext.DDL),
            DBSEntityConstraintType.INHERITANCE);
        this.setPersisted(persisted);
        this.partitionTable = partitionTable;
        this.sequenceNum = sequenceNum;
    }

    @Nullable
    @Override
    public DBSEntityConstraint getReferencedConstraint() {
        return this;
    }

    @Override
    @Property(viewable = true)
    public PostgreTableBase getAssociatedEntity() {
        return this.partitionTable;
    }

    @Property(viewable = true)
    public int getSequenceNum() {
        return sequenceNum;
    }

    @Nullable
    @Override
    public List<PostgreTableForeignKeyColumn> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    void cacheAttributes(DBRProgressMonitor monitor, List<? extends PostgreTableConstraintColumn> children, boolean secondPass) {

    }
    */
}
