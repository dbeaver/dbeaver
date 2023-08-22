// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.text.NumberFormat;
import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;
import org.jkiss.dbeaver.mockdata.engine.generator.AbstractStringValueGenerator;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.Locale;

public class StringPriceGenerator extends AbstractStringValueGenerator
{
    private String country;
    private Locale locale;
    private double min;
    private double max;
    
    public StringPriceGenerator() {
        this.country = "UK";
        this.locale = Locale.UK;
        this.min = 0.0;
        this.max = 1000.0;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final String c = (String) properties.get("country");
        if (!CommonUtils.isEmpty(c)) {
            this.country = c;
            final String s;
            switch (s = c) {
                case "Russia": {
                    this.locale = new Locale("ru", "RU");
                    break;
                }
                case "UK": {
                    this.locale = Locale.UK;
                    break;
                }
                case "USA": {
                    this.locale = Locale.US;
                    break;
                }
                case "China": {
                    this.locale = Locale.CHINA;
                    break;
                }
                case "Italy": {
                    this.locale = Locale.ITALY;
                    break;
                }
                case "Japan": {
                    this.locale = Locale.JAPAN;
                    break;
                }
                case "Germany": {
                    this.locale = Locale.GERMANY;
                    break;
                }
                case "France": {
                    this.locale = Locale.FRANCE;
                    break;
                }
                default:
                    break;
            }
        }
        Double d = (Double) properties.get("min");
        if (d != null) {
            this.min = d;
        }
        d = (Double) properties.get("max");
        if (d != null) {
            this.max = d;
        }
    }
    
    @Override
    protected Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.formatCurrency(MockDataUtils.getRandomDouble(this.min, this.max, this.random));
    }
    
    private String formatCurrency(final double d) {
        final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(this.locale);
        return this.correct(currencyFormatter.format(d));
    }
    
    public String correct(final String str) {
        final int length = str.length();
        if (str.lastIndexOf(44) == length - 1 || str.lastIndexOf(44) == length - 1) {
            return String.valueOf(str) + '0';
        }
        if (str.endsWith(".00") || str.endsWith(",00")) {
            return str.substring(0, length - 3);
        }
        return str;
    }
}
