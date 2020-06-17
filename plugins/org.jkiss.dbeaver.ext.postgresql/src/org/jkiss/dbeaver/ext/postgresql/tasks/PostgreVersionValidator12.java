package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.model.sql.task.SQLToolTaskVersionValidator;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class PostgreVersionValidator12 extends SQLToolTaskVersionValidator<PostgreToolBaseVacuumSettings, DBSObject> {
    @Override
    public int getMajorVersion() {
        return 12;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }
}
