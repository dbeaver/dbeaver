// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.model;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public interface MockValueGenerator
{
    void init( DBSDataManipulator p0,  DBSAttributeBase p1,  Map<String, Object> p2) throws DBException;
    
    void nextRow();
    
    Object generateValue(DBRProgressMonitor p0) throws DBException, IOException;
    
    void dispose();
}
