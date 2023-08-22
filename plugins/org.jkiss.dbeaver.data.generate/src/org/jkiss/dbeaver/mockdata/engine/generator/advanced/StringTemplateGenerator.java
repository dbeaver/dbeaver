// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.util.Arrays;
import java.util.HashMap;
import java.io.IOException;
import java.util.Iterator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.ArrayList;
import java.util.List;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.engine.generator.AbstractStringValueGenerator;
import org.jkiss.dbeaver.mockdata.engine.generator.ConstantGenerator;
import org.jkiss.dbeaver.mockdata.engine.generator.NumericSequenceGenerator;
import org.jkiss.dbeaver.mockdata.engine.model.MockValueGenerator;

public class StringTemplateGenerator extends AbstractStringValueGenerator
{
    private static final Log log;
    private String template;
    private List<MockValueGenerator> generatorsSequence;
    
    static {
        log = Log.getLog((Class)StringTemplateGenerator.class);
    }
    
    public StringTemplateGenerator() {
        this.generatorsSequence = new ArrayList<MockValueGenerator>();
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final String template = (String) properties.get("template");
        if (!CommonUtils.isEmpty(template)) {
            this.template = template;
        }
        final String[] tokens = CommonUtils.splitWithDelimiter(this.template, "${");
        String[] array;
        for (int length = (array = tokens).length, i = 0; i < length; ++i) {
            final String token = array[i];
            if (!CommonUtils.isEmpty(token)) {
                boolean flag = true;
                if (token.startsWith("${")) {
                    final int index = token.lastIndexOf(125);
                    if (index > 0) {
                        final String generatorMarker = token.substring(2, index);
                        final int firstBracketIndex = generatorMarker.indexOf(40);
                        final int lastBracketIndex = generatorMarker.lastIndexOf(41);
                        final boolean hasParameters = firstBracketIndex > 0 && lastBracketIndex > firstBracketIndex;
                        if (hasParameters) {
                            try {
                                final String[] generatorParameters = generatorMarker.substring(firstBracketIndex + 1, lastBracketIndex).split(",");
                                final String generatorName = generatorMarker.substring(0, firstBracketIndex);
                                final String s;
                                switch (s = generatorName) {
                                    case "domain": {
                                        this.generatorsSequence.add(this.initGenerator(new StringDomainGenerator(), container, new Object[0]));
                                        break;
                                    }
                                    case "address": {
                                        this.generatorsSequence.add(this.initGenerator(new StringAddressGenerator(), container, new Object[0]));
                                        break;
                                    }
                                    case "random": {
                                        this.generatorsSequence.add(this.initGenerator(new NumericAdvancedGenerator(), container, "minimum", generatorParameters[0], "maximum", generatorParameters[1], "precision", 10, "scale", 0));
                                        break;
                                    }
                                    case "city": {
                                        this.generatorsSequence.add(this.initGenerator(new StringCityGenerator(), container, new Object[0]));
                                        break;
                                    }
                                    case "name": {
                                        this.generatorsSequence.add(this.initGenerator(new StringNameGenerator(), container, "gender", generatorParameters[0], "withSurnames", generatorParameters[1]));
                                        break;
                                    }
                                    case "email": {
                                        this.generatorsSequence.add(this.initGenerator(new StringEmailGenerator(), container, "gender", generatorParameters[0], "withSurnames", generatorParameters[1], "numericSuffixSize", 0));
                                        break;
                                    }
                                    case "regex": {
                                        this.generatorsSequence.add(this.initGenerator(new StringRegexGenerator(), container, "regex", generatorParameters[0]));
                                        break;
                                    }
                                    case "country": {
                                        this.generatorsSequence.add(this.initGenerator(new StringCountryGenerator(), container, new Object[0]));
                                        break;
                                    }
                                    case "sequence": {
                                        this.generatorsSequence.add(this.initGenerator(new NumericSequenceGenerator(), container, "start", generatorParameters[0], "step", generatorParameters[1]));
                                        break;
                                    }
                                    default:
                                        break;
                                }
                                final String theRest = token.substring(index + 1);
                                if (!CommonUtils.isEmpty(theRest)) {
                                    this.generatorsSequence.add(this.initGenerator(new ConstantGenerator(), container, "value", theRest));
                                }
                                flag = false;
                            }
                            catch (Exception e) {
                                final String message = "Error of the \"" + token + "\" directive of the \"" + template + "\" template processing.";
                                StringTemplateGenerator.log.error((Object)message, (Throwable)e);
                                throw new DBException(message);
                            }
                        }
                    }
                }
                if (flag) {
                    this.generatorsSequence.add(this.initGenerator(new ConstantGenerator(), container, "value", token));
                }
            }
        }
    }
    
    @Override
    protected Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        final StringBuilder sb = new StringBuilder();
        for (final MockValueGenerator generator : this.generatorsSequence) {
            sb.append(generator.generateValue(monitor));
        }
        return sb.toString();
    }
    
    private MockValueGenerator initGenerator(final MockValueGenerator generator, final DBSDataManipulator container, final Object... args) throws DBException {
        final Map<String, Object> properties = new HashMap<String, Object>();
        final Iterator<Object> iterator = Arrays.asList(args).iterator();
        while (iterator.hasNext()) {
            final Object key = iterator.next();
            final Object value = iterator.next();
            properties.put(CommonUtils.toString(key), value);
        }
        properties.put("nulls", 0);
        generator.init(container, this.attribute, properties);
        return generator;
    }
}
