/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mockdata.MockDataUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FKGenerator extends AbstractMockValueGenerator
{
    private static final Log log = Log.getLog(FKGenerator.class);

    private static final int UNIQ_REF_RECORDS_LIMIT = 100000000;
    private static final int REF_RECORDS_LIMIT = 100000;

    private List<Object> refValues = null;

    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        super.init(container, attribute, properties);

        nullsPersent = 0;
/*
        Integer numberRefRecords = (Integer) properties.get("numberRefRecords"); //$NON-NLS-1$
        if (numberRefRecords != null) {
            this.numberRefRecords = numberRefRecords;
        }
*/
    }

    @Override
    public Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (refValues == null) {
            refValues = new ArrayList<>();
            List<DBSEntityReferrer> attributeReferrers = DBUtils.getAttributeReferrers(monitor, (DBSEntityAttribute) attribute);
            if (attributeReferrers.isEmpty()) {
                throw new DBException("Attribute '" + DBUtils.getObjectFullName(attribute, DBPEvaluationContext.UI) + "' is not a part of foreign key");
            }
            DBSEntityReferrer fk = attributeReferrers.get(0); // TODO only the first
            List<? extends DBSEntityAttributeRef> references = ((DBSEntityReferrer) fk).getAttributeReferences(monitor);

            DBSTableForeignKeyColumn column = null;
            for (DBSEntityAttributeRef ref : references) {
                if (((DBPNamedObject) ref).getName().equals(attribute.getName())) {
                    column = (DBSTableForeignKeyColumn) ref;
                }
            }
            if (column == null) {
                throw new DBException("Can't find reference column for '" + attribute.getName() + "'");
            }

            int numberRefRecords = (MockDataUtils.checkUnique(monitor, dbsEntity, attribute) == MockDataUtils.UNIQ_TYPE.SINGLE) ? UNIQ_REF_RECORDS_LIMIT : REF_RECORDS_LIMIT;
            Collection<DBDLabelValuePair> values = readColumnValues(monitor, (DBSAttributeEnumerable) column.getReferencedColumn(), numberRefRecords);
            for (DBDLabelValuePair value : values) {
                refValues.add(value.getValue());
            }
        }
        if (refValues.isEmpty()) {
            return null;
        }
        return refValues.get(random.nextInt(refValues.size()));
    }
}
