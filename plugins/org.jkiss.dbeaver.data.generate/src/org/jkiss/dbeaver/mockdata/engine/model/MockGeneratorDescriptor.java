// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import java.util.Arrays;
import java.util.Iterator;

import org.jkiss.dbeaver.mockdata.engine.internal.MockDataMessages;
import org.jkiss.dbeaver.model.DBPDataKind;
import java.util.Collection;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.utils.CommonUtils;
import java.util.ArrayList;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import java.util.List;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.datatype.DataTypeAbstractDescriptor;

public class MockGeneratorDescriptor extends DataTypeAbstractDescriptor<MockValueGenerator>
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mockGenerator";
    public static final String BOOLEAN_RANDOM_GENERATOR_ID = "booleanRandomGenerator";
    public static final String DATETIME_RANDOM_GENERATOR_ID = "dateRandomGenerator";
    public static final String NUMERIC_ADVANCED_GENERATOR_ID = "numericAdvancedGenerator";
    public static final String STRING_TEXT_GENERATOR_ID = "stringTextGenerator";
    public static final String ZERO_GENERATOR_ID = "zeroGenerator";
    public static final String TAG_PRESET = "preset";
    private Preset preset;
    private String label;
    private String description;
    private final String link;
    private final String url;
    private final DBPImage icon;
    private List<DBPPropertyDescriptor> properties;
    private List<Preset> presets;
    private List<String> tags;
    private String[] replaces;
    
    public MockGeneratorDescriptor(final IConfigurationElement config) {
        super(config, (Class)MockValueGenerator.class);
        this.properties = new ArrayList<DBPPropertyDescriptor>();
        this.presets = new ArrayList<Preset>();
        this.tags = new ArrayList<String>();
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.link = config.getAttribute("link");
        this.url = config.getAttribute("url");
        this.icon = this.iconToImage(config.getAttribute("icon"));
        final String replacesAttr = config.getAttribute("replaces");
        if (!CommonUtils.isEmpty(replacesAttr)) {
            this.replaces = replacesAttr.split("\\.");
        }
        if (!"zeroGenerator".equals(this.getId())) {
            IConfigurationElement[] children;
            for (int length = (children = config.getChildren("propertyGroup")).length, i = 0; i < length; ++i) {
                final IConfigurationElement prop = children[i];
                this.properties.addAll(PropertyDescriptor.extractProperties(prop));
            }
            IConfigurationElement[] children2;
            for (int length2 = (children2 = config.getChildren("preset")).length, j = 0; j < length2; ++j) {
                final IConfigurationElement preset = children2[j];
                this.presets.add(new Preset(preset.getAttribute("id"), preset.getAttribute("label"), preset.getAttribute("mnemonics"), preset.getAttribute("description"), PropertyDescriptor.extractProperties(preset), this.extractTags(preset)));
            }
            if (this.getSupportedTypes().contains(DBPDataKind.STRING)) {
                this.properties.add((DBPPropertyDescriptor)new PropertyDescriptor("General", "lowercase", MockDataMessages.tools_mockdata_prop_lowercase_label, (String)null, false, PropertyDescriptor.PropertyType.t_boolean.getValueType(), (Object)false, (Object[])null));
                this.properties.add((DBPPropertyDescriptor)new PropertyDescriptor("General", "uppercase", MockDataMessages.tools_mockdata_prop_uppercase_label, (String)null, false, PropertyDescriptor.PropertyType.t_boolean.getValueType(), (Object)false, (Object[])null));
            }
            this.properties.add((DBPPropertyDescriptor)new PropertyDescriptor("General", "nulls", MockDataMessages.tools_mockdata_prop_nulls_label, MockDataMessages.tools_mockdata_prop_nulls_description, false, PropertyDescriptor.PropertyType.t_integer.getValueType(), (Object)0, (Object[])null));
        }
        final List<String> tags = this.extractTags(config);
        if (tags != null) {
            this.tags = tags;
        }
    }
    
    public String[] getReplaces() {
        return this.replaces;
    }
    
    public MockGeneratorDescriptor(final IConfigurationElement config, final Preset preset) {
        this(config);
        this.preset = preset;
        this.label = "   " + preset.label;
        if (!CommonUtils.isEmpty(preset.description)) {
            this.description = preset.description;
        }
        for (final DBPPropertyDescriptor prop : preset.getProperties()) {
            this.setDefaultProperty(prop.getId(), prop.getDefaultValue());
        }
        if (preset.getTags() != null) {
            this.tags.addAll(preset.getTags());
        }
        this.presets.clear();
    }
    
    private List<String> extractTags(final IConfigurationElement config) {
        final String tagsAttr = config.getAttribute("tags");
        if (!CommonUtils.isEmpty(tagsAttr)) {
            return Arrays.asList((String[])tagsAttr.split(",").clone());
        }
        return null;
    }
    
    private void setDefaultProperty(final Object id, final Object defaultValue) {
        for (final DBPPropertyDescriptor property : this.properties) {
            if (property.getId().equals(id)) {
                ((PropertyDescriptor)property).setDefaultValue(defaultValue);
                break;
            }
        }
    }
    
    public String getId() {
        if (this.preset != null) {
            return String.valueOf(super.getId()) + "_" + this.preset.id;
        }
        return super.getId();
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public DBPImage getIcon() {
        return this.icon;
    }
    
    public String getLink() {
        return this.link;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public List<DBPPropertyDescriptor> getProperties() {
        return this.properties;
    }
    
    public List<String> getTags() {
        return this.tags;
    }
    
    public DBPPropertyDescriptor getProperty(final Object id) {
        for (final DBPPropertyDescriptor descriptor : this.getProperties()) {
            if (id.equals(descriptor.getId())) {
                return descriptor;
            }
        }
        return null;
    }
    
    public boolean supportsType(final DBSTypedObject typedObject) {
        return typedObject.getDataKind() == DBPDataKind.STRING || super.supportsType(typedObject);
    }
    
    @NotNull
    public MockValueGenerator createGenerator() {
        return (MockValueGenerator)this.createInstance();
    }
    
    public List<Preset> getPresets() {
        return this.presets;
    }
    
    public static class Preset
    {
        private final String id;
        private final String label;
        private final String mnemonics;
        private final String description;
        private final List<DBPPropertyDescriptor> properties;
        private List<String> tags;
        
        public Preset(final String id, final String label, final String mnemonics, final String description, final List<DBPPropertyDescriptor> properties, final List<String> tags) {
            this.tags = new ArrayList<String>();
            this.id = id;
            this.label = label;
            this.mnemonics = mnemonics;
            this.description = description;
            this.properties = properties;
            this.tags = tags;
        }
        
        public String getId() {
            return this.id;
        }
        
        public String getLabel() {
            return this.label;
        }
        
        public String getMnemonics() {
            return this.mnemonics;
        }
        
        public String getDescription() {
            return this.description;
        }
        
        public List<DBPPropertyDescriptor> getProperties() {
            return this.properties;
        }
        
        public List<String> getTags() {
            return this.tags;
        }
    }
}
