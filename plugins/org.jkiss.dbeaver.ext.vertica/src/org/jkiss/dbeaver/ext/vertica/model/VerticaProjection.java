/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.ext.vertica.VerticaUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.util.Collection;
import java.util.Map;

/**
 * VerticaMetaModel
 */
public class VerticaProjection extends JDBCTable<VerticaDataSource, VerticaSchema> implements GenericScriptObject
{
    private static final Log log = Log.getLog(VerticaProjection.class);

    private final String baseName;
    private final String ownerName;
    private final String anchorTableName;
    private final String nodeName;
    private final boolean isPreJoin;
    private final String createType;
    private final String segmentExpression;
    private final String segmentRange;
    private final boolean isSuperProjection;
    private final boolean isKeyConstraintProjection;
    private final boolean hasExpressions;
    private final boolean isAggregateProjection;
    private final String aggregateType;

    public VerticaProjection(VerticaSchema schema, JDBCResultSet dbResult) {
        super(schema, JDBCUtils.safeGetString(dbResult, "projection_name"), true);

        this.baseName = JDBCUtils.safeGetString(dbResult, "projection_basename");
        this.ownerName = JDBCUtils.safeGetString(dbResult, "owner_name");
        this.anchorTableName = JDBCUtils.safeGetString(dbResult, "anchor_table_name");
        this.nodeName = JDBCUtils.safeGetString(dbResult, "node_name");
        this.isPreJoin = JDBCUtils.safeGetBoolean(dbResult, "is_prejoin");
        this.createType = JDBCUtils.safeGetString(dbResult, "create_type");
        this.segmentExpression = JDBCUtils.safeGetString(dbResult, "segment_expression");
        this.segmentRange = JDBCUtils.safeGetString(dbResult, "segment_range");
        this.isSuperProjection = JDBCUtils.safeGetBoolean(dbResult, "is_super_projection");
        this.isKeyConstraintProjection = JDBCUtils.safeGetBoolean(dbResult, "is_key_constraint_projection");
        this.hasExpressions = JDBCUtils.safeGetBoolean(dbResult, "has_expressions");
        this.isAggregateProjection = JDBCUtils.safeGetBoolean(dbResult, "is_aggregate_projection");
        this.aggregateType = JDBCUtils.safeGetString(dbResult, "aggregate_type");
    }

    @Property(viewable = true, order = 10)
    public String getBaseName() {
        return baseName;
    }

    @Property(viewable = true, order = 11)
    public String getOwnerName() {
        return ownerName;
    }

    @Property(viewable = true, order = 12)
    public String getAnchorTableName() {
        return anchorTableName;
    }

    @Property(viewable = true, order = 13)
    public String getNodeName() {
        return nodeName;
    }

    @Property(order = 50)
    public boolean isPreJoin() {
        return isPreJoin;
    }

    @Property(order = 51)
    public String getCreateType() {
        return createType;
    }

    @Property(order = 52)
    public String getSegmentExpression() {
        return segmentExpression;
    }

    @Property(order = 53)
    public String getSegmentRange() {
        return segmentRange;
    }

    @Property(order = 54)
    public boolean isSuperProjection() {
        return isSuperProjection;
    }

    @Property(order = 55)
    public boolean isKeyConstraintProjection() {
        return isKeyConstraintProjection;
    }

    @Property(order = 56)
    public boolean isHasExpressions() {
        return hasExpressions;
    }

    @Property(order = 57)
    public boolean isIsAggregateProjection() {
        return isAggregateProjection;
    }

    @Property(order = 58)
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public JDBCStructCache<VerticaSchema, ? extends DBSEntity, ? extends DBSEntityAttribute> getCache() {
        return getContainer().projectionCache;
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
        return getContainer().projectionCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
        return getContainer().projectionCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public Collection<? extends DBSTableConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getContainer(),
            this);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (isPersisted()) {
            return VerticaUtils.getObjectDDL(monitor, getDataSource(), this);
        } else {
            return null;
        }
    }
}
