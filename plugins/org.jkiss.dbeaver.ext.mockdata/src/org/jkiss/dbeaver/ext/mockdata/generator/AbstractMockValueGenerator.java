package org.jkiss.dbeaver.ext.mockdata.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.model.MockValueGenerator;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.io.IOException;
import java.util.*;

public abstract class AbstractMockValueGenerator implements MockValueGenerator {

    public static final int UNIQUE_VALUES_SET_SIZE = 1000000;

    protected DBSEntity dbsEntity;
    protected DBSAttributeBase attribute;

    protected Random random = new Random();
    protected int nullsPersent = 10;
    private boolean isFirstRun = true;
    private boolean isUnique;
    private Set uniqueValues;

    /**
     * Should be run before the generateValue call
     * @param container
     * @param attribute
     * @param properties
     * @throws DBException
     */
    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        this.dbsEntity = (DBSEntity) container;
        this.attribute = attribute;

        if (attribute.isRequired()) {
            nullsPersent = 0;
        } else {
            if (properties.get("nulls") != null) {
                nullsPersent = (int) properties.get("nulls");
            }
        }
        if (nullsPersent > 100) {
            nullsPersent = 100;
        } else
        if (nullsPersent < 0) {
            nullsPersent = 0;
        }
    }

    @Override
    public void nextRow() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object generateValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isFirstRun) {
            isFirstRun = false;
            isUnique = checkUnique(monitor);
            if (isUnique && (attribute instanceof DBSAttributeEnumerable)) {
                uniqueValues = new HashSet();
                Collection<DBDLabelValuePair> valuePairs = readColumnValues(monitor, dbsEntity.getDataSource(), (DBSAttributeEnumerable) attribute, UNIQUE_VALUES_SET_SIZE);
                for (DBDLabelValuePair pair : valuePairs) {
                    uniqueValues.add(pair.getValue());
                }

            }
        }
        if (isUnique) {
            Object value = null;
            while (value == null || uniqueValues.contains(value)) {
                value = generateOneValue(monitor);
            }
            uniqueValues.add(value);
            return value;
        } else {
            return generateOneValue(monitor);
        }
    }

    protected abstract Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException;

    protected boolean isGenerateNULL() {
        if ((nullsPersent > 0) && ((nullsPersent == 100) || (random.nextInt(100) <= nullsPersent))) {
            return true;
        }
        else {
            return false;
        }
    }

    protected Collection<DBDLabelValuePair> readColumnValues(DBRProgressMonitor monitor, DBPDataSource dataSource, DBSAttributeEnumerable column, int number) throws DBException {
        DBCSession session = DBUtils.openUtilSession(monitor, dataSource, "Read value enumeration");
        return column.getValueEnumeration(session, null, number);
    }

    private boolean checkUnique(DBRProgressMonitor monitor) throws DBException {
        for (DBSEntityConstraint constraint : dbsEntity.getConstraints(monitor)) {
            DBSEntityConstraintType constraintType = constraint.getConstraintType();
            if (constraintType == DBSEntityConstraintType.PRIMARY_KEY || constraintType.isUnique()) {
                DBSEntityAttributeRef constraintAttribute = DBUtils.getConstraintAttribute(monitor, ((DBSEntityReferrer) constraint), attribute.getName());
                if (constraintAttribute != null && constraintAttribute.getAttribute() == attribute) {
                    return true;
                }
            }
        }
        return false;
    }
}
