// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import java.util.ArrayList;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.List;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;

public class FKGenerator extends AbstractMockValueGenerator
{
    private static final Log log;
    private static final int UNIQ_REF_RECORDS_LIMIT = 100000000;
    private static final int REF_RECORDS_LIMIT = 100000;
    private List<Object> refValues;
    
    static {
        log = Log.getLog((Class)FKGenerator.class);
    }
    
    public FKGenerator() {
        this.refValues = null;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        this.nullsPersent = 0;
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.refValues == null) {
            this.refValues = new ArrayList<Object>();
            final List<DBSEntityReferrer> attributeReferrers = (List<DBSEntityReferrer>)DBUtils.getAttributeReferrers(monitor, (DBSEntityAttribute)this.attribute, true);
            if (attributeReferrers.isEmpty()) {
                throw new DBException("Attribute '" + DBUtils.getObjectFullName((DBPNamedObject)this.attribute, DBPEvaluationContext.UI) + "' is not a part of foreign key");
            }
            final DBSEntityReferrer fk = attributeReferrers.get(0);
            final List<? extends DBSEntityAttributeRef> references = (List<? extends DBSEntityAttributeRef>)fk.getAttributeReferences(monitor);
            DBSTableForeignKeyColumn column = null;
            for (final DBSEntityAttributeRef ref : references) {
                if (((DBPNamedObject)ref).getName().equals(this.attribute.getName())) {
                    column = (DBSTableForeignKeyColumn)ref;
                }
            }
            if (column == null) {
                throw new DBException("Can't find reference column for '" + this.attribute.getName() + "'");
            }
            final int numberRefRecords = (MockDataUtils.checkUnique(monitor, this.dbsEntity, this.attribute) == MockDataUtils.UNIQ_TYPE.SINGLE) ? 100000000 : 100000;
            final Collection<DBDLabelValuePair> values = this.readColumnValues(monitor, (DBSAttributeEnumerable)column.getReferencedColumn(), numberRefRecords);
            for (final DBDLabelValuePair value : values) {
                this.refValues.add(value.getValue());
            }
        }
        if (this.refValues.isEmpty()) {
            return null;
        }
        return this.refValues.get(this.random.nextInt(this.refValues.size()));
    }
}
