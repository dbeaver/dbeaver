// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class NullGenerator extends AbstractMockValueGenerator
{
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        return null;
    }
}
