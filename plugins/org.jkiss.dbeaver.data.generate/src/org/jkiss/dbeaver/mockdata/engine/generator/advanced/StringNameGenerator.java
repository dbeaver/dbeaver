// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.List;
import org.jkiss.dbeaver.Log;

public class StringNameGenerator extends AdvancedStringValueGenerator
{
    private static final Log log;
    protected static List<String> MALE_NAMES;
    protected static List<String> FEMALE_NAMES;
    protected static List<String> SURNAMES;
    protected static int maleNames;
    protected static int femaleNames;
    protected static int allNames;
    protected static int surnames;
    private GENDER gender;
    protected boolean withSurnames;
    
    static {
        log = Log.getLog(StringNameGenerator.class);
    }
    
    public StringNameGenerator() {
        this.gender = GENDER.ALL;
        this.withSurnames = false;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final String g = (String) properties.get("gender");
        if (g != null) {
            this.gender = GENDER.valueOf(g);
        }
        Boolean b = this.getBooleanProperty(properties, "withSurnames");
        if (b != null) {
            this.withSurnames = b;
        }
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (StringNameGenerator.allNames == 0) {
            StringNameGenerator.MALE_NAMES = this.readDict("names_male_en.txt");
            StringNameGenerator.FEMALE_NAMES = this.readDict("names_female_en.txt");
            StringNameGenerator.SURNAMES = this.readDict("surnames_en.txt");
            StringNameGenerator.maleNames = StringNameGenerator.MALE_NAMES.size();
            StringNameGenerator.femaleNames = StringNameGenerator.FEMALE_NAMES.size();
            StringNameGenerator.surnames = StringNameGenerator.SURNAMES.size();
            StringNameGenerator.allNames = StringNameGenerator.femaleNames + StringNameGenerator.maleNames;
        }
        if (this.isGenerateNULL()) {
            return null;
        }
        String name = null;
        switch (this.gender) {
            case MALE: {
                name = StringNameGenerator.MALE_NAMES.get(this.random.nextInt(StringNameGenerator.maleNames));
                break;
            }
            case FEMALE: {
                name = StringNameGenerator.FEMALE_NAMES.get(this.random.nextInt(StringNameGenerator.femaleNames));
                break;
            }
            default: {
                final int number = this.random.nextInt(StringNameGenerator.allNames);
                if (number < StringNameGenerator.maleNames) {
                    name = StringNameGenerator.MALE_NAMES.get(number);
                    break;
                }
                name = StringNameGenerator.FEMALE_NAMES.get(number - StringNameGenerator.maleNames);
                break;
            }
        }
        if (this.withSurnames) {
            name = String.valueOf(name) + " " + StringNameGenerator.SURNAMES.get(this.random.nextInt(StringNameGenerator.surnames));
        }
        return this.tune(name);
    }
    
    private enum GENDER
    {
        ALL("ALL", 0), 
        FEMALE("FEMALE", 1), 
        MALE("MALE", 2);
        
        private GENDER(final String name, final int ordinal) {
        }
    }
}
