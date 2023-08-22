// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import org.jkiss.utils.CommonUtils;


import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBUtils;
import java.io.IOException;
import java.util.Iterator;
import java.util.Collection;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import java.util.HashSet;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;
import org.jkiss.dbeaver.mockdata.engine.model.MockValueGenerator;

import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.Set;
import java.util.Random;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;

public abstract class AbstractMockValueGenerator implements MockValueGenerator
{
    public static final int UNIQUE_VALUES_SET_SIZE = 1000000;
    public static final int UNIQUE_VALUE_GEN_ATTEMPTS = 100;
    protected DBSEntity dbsEntity;
    protected DBSAttributeBase attribute;
    protected Random random;
    protected int nullsPersent;
    private boolean isFirstRun;
    private boolean isUnique;
    private Set<Object> uniqueValues;
    
    public AbstractMockValueGenerator() {
        this.random = new Random();
        this.nullsPersent = 10;
        this.isFirstRun = true;
    }
    
    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<String, Object> properties) throws DBException {
        this.dbsEntity = (DBSEntity)container;
        this.attribute = attribute;
        if (attribute.isRequired()) {
            this.nullsPersent = 0;
        }
        else if (properties.get("nulls") != null) {
            this.nullsPersent = (int) properties.get("nulls");
        }
        if (this.nullsPersent > 100) {
            this.nullsPersent = 100;
        }
        else if (this.nullsPersent < 0) {
            this.nullsPersent = 0;
        }
    }
    
    @Override
    public void nextRow() {
    }
    
    @Override
    public void dispose() {
    }
    
    @Override
    public Object generateValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isFirstRun) {
            this.isFirstRun = false;
            this.isUnique = (MockDataUtils.checkUnique(monitor, this.dbsEntity, this.attribute) == MockDataUtils.UNIQ_TYPE.SINGLE);
            if (this.isUnique && this.attribute instanceof DBSAttributeEnumerable) {
                this.uniqueValues = new HashSet<Object>();
                Collection<DBDLabelValuePair> valuePairs = this.readColumnValues(monitor, (DBSAttributeEnumerable)this.attribute, 1000000);
                for (DBDLabelValuePair pair : valuePairs) {
                    this.uniqueValues.add(pair.getValue());
                }
            }
        }
        if (this.isUnique && this.uniqueValues != null) {
            int attempts;
            Object value;
            for (attempts = 0, value = null; value == null || this.uniqueValues.contains(value); value = this.generateOneValue(monitor), ++attempts) {
                if (attempts > 100) {
                    throw new DBException("\n      Can't generate appropriate unique value for the '" + this.attribute.getName() + "' <" + this.attribute.getFullTypeName() + "> attribute.\n" + "      Try to change the generator or its parameters.\n");
                }
                if (monitor.isCanceled()) {
                    return null;
                }
            }
            this.uniqueValues.add(value);
            return value;
        }
        return this.generateOneValue(monitor);
    }
    
    protected abstract Object generateOneValue(final DBRProgressMonitor p0) throws DBException, IOException;
    
    protected boolean isGenerateNULL() {
        return this.nullsPersent > 0 && (this.nullsPersent == 100 || this.random.nextInt(100) <= this.nullsPersent);
    }
    
    protected Collection<DBDLabelValuePair> readColumnValues(final DBRProgressMonitor monitor, final DBSAttributeEnumerable column, final int number) throws DBException {
        final DBCSession session = DBUtils.openUtilSession(monitor, (DBSObject)this.dbsEntity, "Read value enumeration");
        return column.getValueEnumeration(session,null, number, true,false, false); // 是否格式化value
    }
    
    protected Boolean getBooleanProperty(final Map<String, Object> properties, final String propName) {
        final Object prop = properties.get(propName);
        if (prop == null) {
            return null;
        }
        if (prop instanceof Boolean) {
            return (Boolean)prop;
        }
        return CommonUtils.toBoolean(prop);
    }
    
    protected Double getDoubleProperty(final Map<String, Object> properties, final String propName) {
        final Object prop = properties.get(propName);
        if (prop == null) {
            return null;
        }
        if (prop instanceof Double) {
            return (Double)prop;
        }
        return CommonUtils.toDouble(prop);
    }
    
    protected Long getLongProperty(Map<String, Object> properties, String propName) {
        Object prop = properties.get(propName);
        if (prop == null) {
            return null;
        }
        if (prop instanceof Long) {
            return (Long)prop;
        }
        return CommonUtils.toLong(prop);
    }
}
