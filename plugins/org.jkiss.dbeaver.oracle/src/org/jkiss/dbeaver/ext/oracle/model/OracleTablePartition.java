/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * Table partition
 */
public class OracleTablePartition extends OraclePartitionBase<OracleTablePhysical> {

    private List<OracleTablePartition> subPartitions;

    protected OracleTablePartition(
        OracleTablePhysical oracleTable,
        boolean subpartition,
        ResultSet dbResult)
    {
        super(oracleTable, subpartition, dbResult);
    }

    @Association
    public Collection<OracleTablePartition> getSubPartitions()
    {
        return subPartitions;
    }

    public void setSubPartitions(List<OracleTablePartition> subPartitions)
    {
        this.subPartitions = subPartitions;
    }

    public boolean hasSubPartitions()
    {
        return !CommonUtils.isEmpty(subPartitions);
    }

}
