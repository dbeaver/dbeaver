// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui;

import java.util.TreeMap;
import org.jkiss.dbeaver.model.DBPDataKind;
import java.util.Set;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.mockdata.engine.model.MockGeneratorRegistry;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import java.util.List;
import java.util.Iterator;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.ArrayList;
import java.util.HashMap;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import java.util.Collection;
import java.util.Map;

public class EntityProperties
{
    private static final String PROP_REMOVE_OLD_DATA = "removeOldData";
    private static final String PROP_ROWS_NUMBER = "rowsNumber";
    private static final String PROP_BATCH_SIZE = "batchSize";
    private static final String KEY_SELECTED_ATTRIBUTE = "selectedAttribute";
    private static final String KEY_SELECTED_GENERATOR = "selectedGenerator";
    private static final String KEY_PRESET_ID = "presetId";
    private static final String KEY_GENERATOR_SECTION = "GENERATOR_SECTION";
    static final String NO_GENERATOR_ID = "<no generator>";
    private final Map<String, MockGeneratorDescriptor> generatorDescriptors;
    private final Map<String, AttributeProperties> attributeGenerators;
    private final Collection<DBSAttributeBase> attributes;
    private final DBSObject inputObject;
    private final DBSDataManipulator dataManipulator;
    private final DBSEntity entity;
    @Nullable
    private String selectedAttribute;
    private boolean removeOldData;
    private long rowsNumber;
    private int batchSize;
    
    EntityProperties(@NotNull final DBSObject inputObject) {
        this.generatorDescriptors = new HashMap<String, MockGeneratorDescriptor>();
        this.attributeGenerators = new HashMap<String, AttributeProperties>();
        this.attributes = new ArrayList<DBSAttributeBase>();
        this.rowsNumber = 1000L;
        this.batchSize = 200;
        this.inputObject = inputObject;
        this.dataManipulator = (DBSDataManipulator)inputObject;
        this.entity = (DBSEntity)inputObject;
    }
    
    public void init(final DBRProgressMonitor monitor) throws DBException {
        for (final DBSAttributeBase attr : CommonUtils.safeCollection(this.entity.getAttributes(monitor))) {
            if (!DBUtils.isPseudoAttribute(attr)) {
                if (DBUtils.isHiddenObject((Object)attr)) {
                    continue;
                }
                this.attributes.add(attr);
            }
        }
        final MockGeneratorRegistry generatorRegistry = MockGeneratorRegistry.getInstance();
        for (final DBSAttributeBase attribute : this.attributes) {
            final AttributeProperties generatorProperties = new AttributeProperties(attribute);
            this.attributeGenerators.put(attribute.getName(), generatorProperties);
            final List<DBSEntityReferrer> attributeReferrers = (List<DBSEntityReferrer>)DBUtils.getAttributeReferrers(monitor, (DBSEntityAttribute)attribute, true);
            if (!CommonUtils.isEmpty((Collection)attributeReferrers)) {
                final MockGeneratorDescriptor generator = generatorRegistry.getGenerator("fkGenerator");
                this.putGenerator(generatorProperties, generator);
            }
            else {
                final List<MockGeneratorDescriptor> generators = (List<MockGeneratorDescriptor>)generatorRegistry.findAllGenerators(this.dataManipulator.getDataSource(), (DBSTypedObject)attribute);
                for (final MockGeneratorDescriptor generator2 : generators) {
                    this.putGenerator(generatorProperties, generator2);
                }
            }
        }
    }
    
    DBSObject getInputObject() {
        return this.inputObject;
    }
    
    DBSDataManipulator getDataManipulator() {
        return this.dataManipulator;
    }
    
    DBSEntity getEntity() {
        return this.entity;
    }
    
    Collection<DBSAttributeBase> getAttributes() {
        return this.attributes;
    }
    
    @Nullable
    String getSelectedAttribute() {
        return this.selectedAttribute;
    }
    
    void setSelectedAttribute(@Nullable final String selectedAttribute) {
        this.selectedAttribute = selectedAttribute;
    }
    
    private void putGenerator(final AttributeProperties generatorProperties, final MockGeneratorDescriptor generator) {
        this.generatorDescriptors.put(generator.getId(), generator);
        generatorProperties.putGeneratorProperties(generator.getId(), new PropertySourceCustom((Collection)generator.getProperties(), (Map)null));
    }
    
    public MockGeneratorDescriptor getGeneratorDescriptor(final String generatorId) {
        return this.generatorDescriptors.get(generatorId);
    }
    
    @Nullable
    public MockGeneratorDescriptor findGeneratorForName(final DBSAttributeBase attribute, final String generatorName) {
        final AttributeProperties attributeGeneratorProperties = this.attributeGenerators.get(attribute.getName());
        for (final String generatorId : attributeGeneratorProperties.getGenerators()) {
            final MockGeneratorDescriptor generatorDescriptor = this.generatorDescriptors.get(generatorId);
            if (generatorName.equals(generatorDescriptor.getLabel())) {
                return generatorDescriptor;
            }
        }
        return null;
    }
    
    public boolean isRemoveOldData() {
        return this.removeOldData;
    }
    
    public void setRemoveOldData(final boolean removeOldData) {
        this.removeOldData = removeOldData;
    }
    
    public long getRowsNumber() {
        return this.rowsNumber;
    }
    
    public void setRowsNumber(final long rowsNumber) {
        this.rowsNumber = rowsNumber;
    }
    
    public int getBatchSize() {
        return this.batchSize;
    }
    
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }
    
    public Map<String, AttributeProperties> getAttributeGenerators() {
        return this.attributeGenerators;
    }
    
    public AttributeProperties getAttributeProperties(@NotNull final DBSAttributeBase attribute) {
        return this.attributeGenerators.get(attribute.getName());
    }
    
    private IDialogSettings getEntitySettings(@NotNull final IDialogSettings dialogSettings) {
        return UIUtils.getSettingsSection(dialogSettings, String.valueOf(this.entity.getDataSource().getName()) + "." + DBUtils.getObjectFullName(this.entity.getDataSource(), (DBPNamedObject)this.entity, DBPEvaluationContext.DML));
    }
    
    public void loadFrom(@NotNull final DBRProgressMonitor monitor, @NotNull final IDialogSettings dialogSettings) {
        final IDialogSettings entitySettings = this.getEntitySettings(dialogSettings);
        this.removeOldData = entitySettings.getBoolean("removeOldData");
        try {
            this.rowsNumber = entitySettings.getInt("rowsNumber");
        }
        catch (NumberFormatException ex) {}
        try {
            this.batchSize = entitySettings.getInt("batchSize");
        }
        catch (NumberFormatException ex2) {}
        this.selectedAttribute = entitySettings.get("selectedAttribute");
        for (final Map.Entry<String, AttributeProperties> entry : this.attributeGenerators.entrySet()) {
            final String attributeName = entry.getKey();
            final IDialogSettings attributeSection = UIUtils.getSettingsSection(entitySettings, attributeName);
            final String savedGeneratorId = attributeSection.get("selectedGenerator");
            final AttributeProperties attrGeneratorProperties = entry.getValue();
            if ("<no generator>".equals(savedGeneratorId)) {
                attrGeneratorProperties.setSelectedGenerator(null);
                attrGeneratorProperties.setPresetId(null);
            }
            else if (!CommonUtils.isEmpty(savedGeneratorId)) {
                final MockGeneratorDescriptor generatorDescriptor = attrGeneratorProperties.getGenerator(savedGeneratorId);
                if (generatorDescriptor != null) {
                    attrGeneratorProperties.setSelectedGenerator(generatorDescriptor);
                    attrGeneratorProperties.setPresetId(attributeSection.get("presetId"));
                }
                final PropertySourceCustom generatorPropertySource = attrGeneratorProperties.getGeneratorProperties();
                final IDialogSettings generatorSection = UIUtils.getSettingsSection(attributeSection, "GENERATOR_SECTION");
                if (generatorPropertySource == null) {
                    continue;
                }
                DBPPropertyDescriptor[] properties;
                for (int length = (properties = generatorPropertySource.getProperties()).length, i = 0; i < length; ++i) {
                    final DBPPropertyDescriptor prop = properties[i];
                    final String key = prop.getId();
                    final Object savedValue = UIUtils.getSectionValueWithType(generatorSection, key);
                    if (!key.equals("nulls") || !(savedValue instanceof Boolean)) {
                        generatorPropertySource.setPropertyValue(monitor, key, savedValue);
                    }
                }
            }
            else {
                this.autoAssignGenerator(attrGeneratorProperties);
            }
        }
    }
    
    public void autoAssignGenerator(final AttributeProperties attrGeneratorProperties) {
        final DBSAttributeBase attribute = attrGeneratorProperties.getAttribute();
        if (attribute.isAutoGenerated()) {
            attrGeneratorProperties.setSelectedGenerator(null);
            return;
        }
        final String attributeName = attribute.getName().toLowerCase();
        final Set<String> attrGeneratorIds = attrGeneratorProperties.getGenerators();
        boolean found = false;
        for (final String generatorId : attrGeneratorIds) {
            final MockGeneratorDescriptor generatorDescriptor = this.getGeneratorDescriptor(generatorId);
            for (final String tag : generatorDescriptor.getTags()) {
                if (attributeName.contains(tag)) {
                    attrGeneratorProperties.setSelectedGenerator(generatorDescriptor);
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found) {
            if (attrGeneratorIds.contains("fkGenerator")) {
                this.setSelectedGenerator(attrGeneratorProperties, "fkGenerator");
                return;
            }
            switch (attribute.getDataKind()) {
                case BOOLEAN: {
                    this.setSelectedGenerator(attrGeneratorProperties, "booleanRandomGenerator");
                    break;
                }
                case DATETIME: {
                    this.setSelectedGenerator(attrGeneratorProperties, "dateRandomGenerator");
                    break;
                }
                case NUMERIC: {
                    this.setSelectedGenerator(attrGeneratorProperties, "numericAdvancedGenerator");
                    break;
                }
                case STRING: {
                    this.setSelectedGenerator(attrGeneratorProperties, "stringTextGenerator");
                    break;
                }
            }
        }
    }
    
    private void setSelectedGenerator(@NotNull final AttributeProperties attrGeneratorProperties, @Nullable final String generatorId) {
        if (generatorId == null) {
            attrGeneratorProperties.setSelectedGenerator(null);
        }
        else {
            attrGeneratorProperties.setSelectedGenerator(attrGeneratorProperties.getGenerator(generatorId));
        }
    }
    
    void saveTo(@NotNull final IDialogSettings dialogSettings) {
        final IDialogSettings entitySettings = this.getEntitySettings(dialogSettings);
        entitySettings.put("removeOldData", this.removeOldData);
        entitySettings.put("rowsNumber", this.rowsNumber);
        entitySettings.put("batchSize", this.batchSize);
        entitySettings.put("selectedAttribute", this.selectedAttribute);
        for (final Map.Entry<String, AttributeProperties> attrEntry : this.attributeGenerators.entrySet()) {
            final String attributeName = attrEntry.getKey();
            final AttributeProperties attrGeneratorProperties = attrEntry.getValue();
            final IDialogSettings attributeSection = UIUtils.getSettingsSection(entitySettings, attributeName);
            final MockGeneratorDescriptor selectedGenerator = attrGeneratorProperties.getSelectedGenerator();
            if (selectedGenerator == null) {
                attributeSection.put("selectedGenerator", "<no generator>");
                attributeSection.put("presetId", (String)null);
            }
            else {
                attributeSection.put("selectedGenerator", selectedGenerator.getId());
                attributeSection.put("presetId", attrGeneratorProperties.getPresetId());
                final IDialogSettings generatorSection = UIUtils.getSettingsSection(attributeSection, "GENERATOR_SECTION");
                final PropertySourceCustom generatorPropertySource = attrGeneratorProperties.getGeneratorProperties();
                if (generatorPropertySource == null) {
                    continue;
                }
                final Map<String, Object> properties = (Map<String, Object>)generatorPropertySource.getPropertiesWithDefaults();
                for (final Map.Entry<String, Object> propEntry : properties.entrySet()) {
                    UIUtils.putSectionValueWithType(generatorSection, (String)propEntry.getKey(), propEntry.getValue());
                }
            }
        }
    }
    
    class AttributeProperties
    {
        private final Map<String, PropertySourceCustom> generators;
        private final DBSAttributeBase attribute;
        @Nullable
        private MockGeneratorDescriptor selectedGenerator;
        @Nullable
        private String presetId;
        
        public AttributeProperties(final DBSAttributeBase attribute) {
            this.generators = new TreeMap<String, PropertySourceCustom>();
            this.attribute = attribute;
        }
        
        public DBSAttributeBase getAttribute() {
            return this.attribute;
        }
        
        @Nullable
        public MockGeneratorDescriptor getSelectedGenerator() {
            return this.selectedGenerator;
        }
        
        public Set<String> getGenerators() {
            return this.generators.keySet();
        }
        
        public void setSelectedGenerator(@Nullable final MockGeneratorDescriptor generator) {
            this.selectedGenerator = generator;
        }
        
        public void putGeneratorProperties(final String generatorId, final PropertySourceCustom propertySource) {
            this.generators.put(generatorId, propertySource);
        }
        
        public PropertySourceCustom getGeneratorProperties() {
            return (this.selectedGenerator == null) ? null : this.generators.get(this.selectedGenerator.getId());
        }
        
        @Nullable
        public String getPresetId() {
            return this.presetId;
        }
        
        public void setPresetId(@Nullable final String presetId) {
            this.presetId = presetId;
        }
        
        public boolean isEmpty() {
            return this.generators.isEmpty();
        }
        
        public MockGeneratorDescriptor getGenerator(final String generatorId) {
            return EntityProperties.this.generatorDescriptors.get(generatorId);
        }
    }
}
