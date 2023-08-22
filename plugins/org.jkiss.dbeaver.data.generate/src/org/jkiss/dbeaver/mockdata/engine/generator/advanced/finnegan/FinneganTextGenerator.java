// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.finnegan;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.lang.reflect.Field;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.generator.advanced.AdvancedStringValueGenerator;

import java.util.Locale;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public class FinneganTextGenerator extends AdvancedStringValueGenerator
{
    private Finnegan finnegan;
    private String schema;
    private int minWordCount;
    private int maxWordCount;
    private long startSeed;
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        this.schema = CommonUtils.toString(properties.get("schema"));
        this.minWordCount = CommonUtils.toInt(properties.get("minWordCount"), 5);
        this.maxWordCount = CommonUtils.toInt(properties.get("maxWordCount"), 10);
        this.startSeed = System.currentTimeMillis();
        try {
            final Field field = Finnegan.class.getField(this.schema.toUpperCase(Locale.ENGLISH));
            this.finnegan = (Finnegan)field.get(null);
        }
        catch (Throwable e) {
            throw new DBException("Bad schema: " + this.schema, e);
        }
    }
    
    @Override
    protected Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        return this.finnegan.sentence(this.startSeed++, this.minWordCount, this.maxWordCount, new String[] { ",", ",", ",", ";" }, new String[] { ".", ".", ".", "!", "?", "..." }, 0.17);
    }
}
