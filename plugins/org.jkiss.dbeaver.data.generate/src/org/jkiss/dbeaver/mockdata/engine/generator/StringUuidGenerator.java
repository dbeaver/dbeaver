// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.DBException;
import java.util.UUID;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class StringUuidGenerator extends AbstractStringValueGenerator
{
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.tune(UUID.randomUUID().toString());
    }
}
