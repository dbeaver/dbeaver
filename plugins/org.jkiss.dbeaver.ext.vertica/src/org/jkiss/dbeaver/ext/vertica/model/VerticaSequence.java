/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp
 *
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of DBeaver Corp and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to DBeaver Corp and its suppliers
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from DBeaver Corp.
 */
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class VerticaSequence extends GenericSequence {

    private static final Log log = Log.getLog(VerticaSequence.class);

    private String name;
    private String identityTableName;
    private long cacheCount;
    private boolean isCycle;
    private VerticaSchema schema;
    private String description;

    public VerticaSequence(GenericStructContainer container, String name, String description, Number lastValue, Number minValue, Number maxValue, Number incrementBy, String identityTableName, long cacheCount, boolean isCycle) {
        super(container, name, description, lastValue, minValue, maxValue, incrementBy);
        this.name = name;
        this.identityTableName = identityTableName;
        this.cacheCount = cacheCount;
        this.isCycle = isCycle;
        this.schema = (VerticaSchema) container.getSchema();
        this.description = description;
    }

    public VerticaSequence(GenericStructContainer container, String name) {
        super(container, name, null, 0, 1, 9223372036854775807L, 1);
        this.schema = (VerticaSchema) container.getSchema();
        this.cacheCount = 25000;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 11)
    public GenericTableBase getIdentityTableName(DBRProgressMonitor monitor) {
        GenericTableBase table = null;
        if (CommonUtils.isEmpty(identityTableName)) {
            return null;
        }
        try {
            table = schema.getTable(monitor, identityTableName);
        } catch (DBException e) {
            log.debug("Can't find identity table", e);
        }
        return table;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public long getCacheCount() {
        return cacheCount;
    }

    public void setCacheCount(long cacheCount) {
        this.cacheCount = cacheCount;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public boolean isCycle() {
        return isCycle;
    }

    public void setCycle(boolean cycle) {
        isCycle = cycle;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, editable = true, updatable = true, order = 10)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



}
