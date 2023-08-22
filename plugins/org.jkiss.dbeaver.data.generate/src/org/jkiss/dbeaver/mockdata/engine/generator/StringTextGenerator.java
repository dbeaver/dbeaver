// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public class StringTextGenerator extends AbstractStringValueGenerator
{
    private String templateString;
    private int minLength;
    private int maxLength;
    
    public StringTextGenerator() {
        this.minLength = 1;
        this.maxLength = 100;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        this.templateString = CommonUtils.toString(properties.get("template"));
        if (this.templateString == null) {
            throw new DBCException("Empty template string for simple string generator");
        }
        Integer min = (Integer) properties.get("minLength");
        if (min != null) {
            this.minLength = min;
        }
        if (this.minLength > this.templateString.length()) {
            this.minLength = this.templateString.length();
        }
        Integer max = (Integer) properties.get("maxLength");
        if (max != null) {
            this.maxLength = max;
        }
        if (this.maxLength == 0 || (attribute.getMaxLength() > 0L && this.maxLength > attribute.getMaxLength())) {
            this.maxLength = (int)attribute.getMaxLength();
        }
        if (this.maxLength > this.templateString.length()) {
            this.maxLength = this.templateString.length();
        }
        if (this.minLength > this.maxLength) {
            this.maxLength = this.minLength;
        }
    }
    
    @Override
    public void nextRow() {
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        final int length = this.minLength + this.random.nextInt(this.maxLength - this.minLength + 1);
        final int tplLength = this.templateString.length();
        int start = this.random.nextInt(tplLength);
        if (start > 0) {
            int wordStart;
            for (wordStart = start; wordStart < tplLength && !Character.isWhitespace(this.templateString.charAt(wordStart - 1)); ++wordStart) {}
            if (wordStart < tplLength) {
                start = wordStart;
            }
        }
        if (start + length < tplLength) {
            return this.tune(this.templateString.substring(start, start + length));
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(this.templateString.substring(start));
        final int newlength = length - (tplLength - start);
        for (int i = 0; i < newlength / tplLength; ++i) {
            sb.append(this.templateString);
        }
        sb.append(this.templateString.substring(0, newlength % tplLength));
        return this.tune(sb.toString().trim());
    }
}
