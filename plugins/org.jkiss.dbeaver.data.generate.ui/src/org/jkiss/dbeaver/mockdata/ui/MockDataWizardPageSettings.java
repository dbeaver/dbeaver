// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui;

import java.util.Map;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.eclipse.swt.events.ModifyEvent;
import org.jkiss.utils.CommonUtils;
import org.eclipse.swt.widgets.Event;
import java.lang.reflect.InvocationTargetException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import java.util.ArrayList;
import java.util.function.Function;
import org.eclipse.swt.widgets.Widget;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collection;
import java.util.Optional;
import org.jkiss.code.NotNull;
import org.eclipse.swt.widgets.Group;
import java.util.function.Consumer;
import org.eclipse.swt.widgets.TreeItem;
import java.util.Objects;
import org.jkiss.dbeaver.model.DBPImage;
import java.util.Iterator;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.custom.SashForm;
import org.jkiss.dbeaver.ui.UIUtils;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.engine.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.mockdata.ui.internal.MockDataUIMessages;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

public class MockDataWizardPageSettings extends ActiveWizardPage<MockDataExecuteWizard>
{
    private static final Log log;
    private static final int DEFAULT_NAME_COLUMN_WIDTH = 110;
    private final MockDataSettings mockDataSettings;
    private Tree overviewTree;
    private Combo generatorCombo;
    private PropertyTreeViewer propertiesEditor;
    private Text entityNameText;
    private Button removeOldDataCheck;
    private Text rowsText;
    private Text batchSizeText;
    private boolean firstInit;
    private transient boolean loadingSettings;
    private DBSEntity selectedEntity;
    @Nullable
    private DBSAttributeBase selectedAttribute;
    @Nullable
    private PropertySourceCustom propertySource;
    
    static {
        log = Log.getLog((Class)MockDataWizardPageSettings.class);
    }
    
    MockDataWizardPageSettings(final MockDataSettings mockDataSettings) {
        super(MockDataUIMessages.tools_mockdata_wizard_page_settings_page_name);
        this.setTitle(MockDataUIMessages.tools_mockdata_wizard_page_settings_page_name);
        this.setDescription(MockDataUIMessages.tools_mockdata_wizard_page_settings_page_description);
        this.mockDataSettings = mockDataSettings;
        this.firstInit = true;
    }
    
    public void createControl(final Composite parent) {
        final Composite composite = UIUtils.createComposite(parent, 1);
        final SashForm sash = new SashForm(composite, 256);
        sash.setLayoutData((Object)new GridData(1808));
        (this.overviewTree = new Tree(UIUtils.createPlaceholder((Composite)sash, 1), 67584)).setLayoutData((Object)new GridData(4, 4, true, true));
        UIUtils.createTreeColumn(this.overviewTree, 16384, MockDataUIMessages.tools_mockdata_wizard_page_settings_overview_tree_column_database_objects);
        UIUtils.createTreeColumn(this.overviewTree, 131072, MockDataUIMessages.tools_mockdata_wizard_page_settings_overview_tree_column_generators);
        this.overviewTree.setHeaderVisible(true);
        final Composite settingsComposite = UIUtils.createComposite((Composite)sash, 1);
        final Composite generatorPropertiesComposite = UIUtils.createComposite(settingsComposite, 1);
        final Composite entitySettingsComposite = new Composite(settingsComposite, 2048);
        final StackLayout settingsCompositeLayout = new StackLayout();
        settingsComposite.setLayout((Layout)settingsCompositeLayout);
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        entitySettingsComposite.setLayout((Layout)layout);
        generatorPropertiesComposite.setLayoutData((Object)new GridData(1808));
        final Composite comboAndResetButtonComposite = UIUtils.createComposite(generatorPropertiesComposite, 3);
        comboAndResetButtonComposite.setLayoutData((Object)new GridData(768));
        (this.generatorCombo = new Combo(comboAndResetButtonComposite, 12)).addSelectionListener((SelectionListener)new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                if (MockDataWizardPageSettings.this.selectedAttribute == null) {
                    MockDataWizardPageSettings.log.warn((Object)"Attribute is null when clicking on generator combo. There is no target for the chosen generator!");
                    return;
                }
                MockDataWizardPageSettings.this.selectGenerator(MockDataWizardPageSettings.this.selectedEntity, MockDataWizardPageSettings.this.selectedAttribute, MockDataWizardPageSettings.this.generatorCombo.getText());
            }
        });
        final GridData gd = new GridData();
        gd.widthHint = UIUtils.getFontHeight((Control)this.generatorCombo) * 20;
        this.generatorCombo.setLayoutData((Object)gd);
        UIUtils.createEmptyLabel(comboAndResetButtonComposite, 1, 1).setLayoutData((Object)new GridData(768));
        UIUtils.createDialogButton(comboAndResetButtonComposite, MockDataUIMessages.tools_mockdata_wizard_page_settings_button_reset, (DBPImage)null, MockDataUIMessages.tools_mockdata_wizard_page_settings_button_reset_tooltip, (SelectionListener)new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                if (MockDataWizardPageSettings.this.propertySource == null) {
                    if (MockDataWizardPageSettings.this.selectedAttribute != null) {
                        MockDataWizardPageSettings.log.debug((Object)"Property source is null but selected attribute is not");
                    }
                    return;
                }
                for (final String key : MockDataWizardPageSettings.this.propertySource.getPropertyValues().keySet()) {
                    MockDataWizardPageSettings.this.propertySource.resetPropertyValueToDefault(key);
                }
                MockDataWizardPageSettings.this.propertiesEditor.loadProperties((DBPPropertySource)MockDataWizardPageSettings.this.propertySource);
                MockDataWizardPageSettings.this.setPropertiesEditorColumnsWidth();
                MockDataWizardPageSettings.this.refreshOverviewTree();
            }
        });
        this.propertiesEditor = new PropertyTreeViewer(generatorPropertiesComposite, 2048);
        this.propertiesEditor.getControl().setLayoutData((Object)new GridData(1808));
        final SelectionListener changeListener = (SelectionListener)new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                MockDataWizardPageSettings.this.updateState(MockDataWizardPageSettings.this.selectedEntity);
            }
        };
        final Composite entityComposite = UIUtils.createComposite(entitySettingsComposite, 2);
        this.entityNameText = UIUtils.createLabelText(entityComposite, MockDataUIMessages.tools_mockdata_wizard_page_settings_text_entity, "", 8);
        GridData gd2 = new GridData(768);
        gd2.verticalIndent = 5;
        gd2.horizontalIndent = 9;
        entityComposite.setLayoutData((Object)gd2);
        final Group settingsGroup = UIUtils.createControlGroup(entitySettingsComposite, MockDataUIMessages.tools_mockdata_wizard_page_settings_group_settings, 4, 768, 0);
        gd2 = new GridData(768);
        gd2.verticalIndent = 5;
        settingsGroup.setLayoutData((Object)gd2);
        (this.removeOldDataCheck = UIUtils.createCheckbox((Composite)settingsGroup, MockDataUIMessages.tools_mockdata_wizard_page_settings_checkbox_remove_old_data, (String)null, true, 4)).addSelectionListener(changeListener);
        (this.rowsText = UIUtils.createLabelText((Composite)settingsGroup, MockDataUIMessages.tools_mockdata_wizard_page_settings_combo_rows, String.valueOf(0), 2048, (Object)new GridData(110, -1))).addSelectionListener(changeListener);
        this.rowsText.addVerifyListener(UIUtils.getUnsignedLongOrEmptyTextVerifyListener(this.rowsText));
        this.rowsText.addModifyListener(e -> this.updateState(this.selectedEntity));
        (this.batchSizeText = UIUtils.createLabelText((Composite)settingsGroup, MockDataUIMessages.tools_mockdata_wizard_page_settings_batch_size, String.valueOf(0), 2048, (Object)new GridData(110, -1))).addSelectionListener(changeListener);
        this.batchSizeText.addVerifyListener(UIUtils.getUnsignedLongOrEmptyTextVerifyListener(this.batchSizeText));
        this.batchSizeText.addModifyListener(e -> this.updateState(this.selectedEntity));
        this.overviewTree.addSelectionListener((SelectionListener)new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                MockDataWizardPageSettings.this.saveGeneratorProperties();
                final Object data = e.item.getData();
                if (data instanceof DBSEntity) {
                    if (!Objects.equals(MockDataWizardPageSettings.this.selectedEntity, data)) {
                        MockDataWizardPageSettings.this.updateState(MockDataWizardPageSettings.this.selectedEntity);
                    }
                    MockDataWizardPageSettings.access$11(MockDataWizardPageSettings.this, (DBSEntity)data);
                    MockDataWizardPageSettings.access$12(MockDataWizardPageSettings.this, null);
                    MockDataWizardPageSettings.access$13(MockDataWizardPageSettings.this, null);
                    setCompositeOnTop(settingsComposite, settingsCompositeLayout, entitySettingsComposite);
                    MockDataWizardPageSettings.this.updateGeneralSettings(MockDataWizardPageSettings.this.selectedEntity);
                    return;
                }
                final DBSAttributeBase attribute = (DBSAttributeBase)data;
                final TreeItem attributeItem = (TreeItem)e.item;
                final TreeItem entityItem = attributeItem.getParentItem();
                final DBSEntity entity = (DBSEntity)entityItem.getData();
                final EntityProperties entityProperties = MockDataWizardPageSettings.this.mockDataSettings.getEntityProperties(entity);
                if (entityProperties == null) {
                    MockDataWizardPageSettings.log.debug((Object)("Unable to correctly select attribute " + attribute + ": corresponding entity settings not found"));
                    return;
                }
                final EntityProperties.AttributeProperties attributeProperties = entityProperties.getAttributeProperties(attribute);
                final MockGeneratorDescriptor descriptor = attributeProperties.getSelectedGenerator();
                MockDataWizardPageSettings.this.reloadProperties(entity, attribute, (descriptor == null) ? null : descriptor.getId());
                setCompositeOnTop(settingsComposite, settingsCompositeLayout, generatorPropertiesComposite);
            }
        });
        sash.setWeights(new int[] { 40, 60 });
        final Composite buttonsGroup = new Composite(composite, 0);
        buttonsGroup.setLayoutData((Object)new GridData(768));
        buttonsGroup.setLayout((Layout)new GridLayout(4, false));
        UIUtils.createDialogButton(buttonsGroup, MockDataUIMessages.tools_mockdata_wizard_page_settings_button_autoassign, (DBPImage)null, MockDataUIMessages.tools_mockdata_wizard_page_settings_button_autoassign_tooltip, (SelectionListener)new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                for (final EntityProperties entityProperties : MockDataWizardPageSettings.this.mockDataSettings.getEntityPropertiesList()) {
                    entityProperties.getAttributeGenerators().values().forEach(entityProperties::autoAssignGenerator);
                }
                MockDataWizardPageSettings.this.refreshOverviewTree();
                if (MockDataWizardPageSettings.this.selectedAttribute == null) {
                    return;
                }
                MockDataWizardPageSettings.this.selectGenerator(MockDataWizardPageSettings.this.selectedEntity, MockDataWizardPageSettings.this.selectedAttribute, MockDataWizardPageSettings.this.getAttributeGeneratorLabel(MockDataWizardPageSettings.this.selectedEntity, MockDataWizardPageSettings.this.selectedAttribute));
            }
        });
        UIUtils.createEmptyLabel(buttonsGroup, 1, 1).setLayoutData((Object)new GridData(768));
        UIUtils.createDialogButton(buttonsGroup, MockDataUIMessages.tools_mockdata_wizard_page_settings_button_summary, (DBPImage)null, MockDataUIMessages.tools_mockdata_wizard_page_settings_button_summary_tooltip, (SelectionListener)new SelectionAdapter() {
            public void widgetSelected(final SelectionEvent e) {
                final ShowSummaryDialog dialog = new ShowSummaryDialog(e.display.getActiveShell(), mockDataSettings);
                dialog.open();
            }
        });
        this.setControl((Control)composite);
    }
    
    private static void setCompositeOnTop(final Composite compositeWithStackLayout, final StackLayout layout, final Composite composite) {
        if (layout.topControl == composite) {
            return;
        }
        layout.topControl = (Control)composite;
        compositeWithStackLayout.layout();
    }
    
    private void selectGenerator(@NotNull final DBSEntity entity, @NotNull final DBSAttributeBase attribute, @NotNull final String generatorName) {
        if (generatorName.isEmpty()) {
            return;
        }
        if (MockDataSettings.NO_GENERATOR_LABEL.equals(generatorName)) {
            this.reloadProperties(entity, attribute, MockDataSettings.NO_GENERATOR_LABEL);
            this.updateGeneratorTextInOverviewTree(entity, attribute);
            return;
        }
        final Optional<EntityProperties> opt = this.mockDataSettings.getEntityPropertiesList().stream().filter(properties -> properties.getEntity().equals(entity)).findAny();
        if (!opt.isPresent()) {
            MockDataWizardPageSettings.log.debug((Object)("Unable to select generator, no properties found for entity " + entity));
            return;
        }
        final MockGeneratorDescriptor generatorForName = opt.get().findGeneratorForName(attribute, generatorName);
        if (generatorForName == null) {
            return;
        }
        this.saveGeneratorProperties();
        this.reloadProperties(entity, attribute, generatorForName.getId());
        this.updateGeneratorTextInOverviewTree(entity, attribute);
    }
    
    private void updateGeneratorTextInOverviewTree(@NotNull final DBSEntity entity, @NotNull final DBSAttributeBase attributeBase) {
        final String label = this.getAttributeGeneratorLabel(entity, attributeBase);
        TreeItem entityItem = null;
        TreeItem[] items;
        for (int length = (items = this.overviewTree.getItems()).length, i = 0; i < length; ++i) {
            final TreeItem item = items[i];
            if (entity.equals(item.getData())) {
                entityItem = item;
                break;
            }
        }
        if (entityItem == null) {
            MockDataWizardPageSettings.log.debug((Object)("Unable to update generator text in overview tre: unable to find entity " + entity));
            return;
        }
        TreeItem attributeItem = null;
        TreeItem[] items2;
        for (int length2 = (items2 = entityItem.getItems()).length, j = 0; j < length2; ++j) {
            final TreeItem item2 = items2[j];
            if (attributeBase.equals(item2.getData())) {
                attributeItem = item2;
                break;
            }
        }
        if (attributeItem == null) {
            MockDataWizardPageSettings.log.debug((Object)("Unable to update generator text in overview tree: unable to find attribute " + attributeBase));
            return;
        }
        attributeItem.setText(1, label);
        this.overviewTree.redraw();
    }
    
    private void refreshOverviewTree() {
        final Collection<DBSEntity> expandedEntities = Stream.of(this.overviewTree.getItems()).filter(TreeItem::getExpanded).map(treeItem -> (DBSEntity)treeItem.getData()).collect(Collectors.toSet());
        final List<Object> selectedObjects = Stream.of(this.overviewTree.getSelection()).map((Function<? super TreeItem, ?>)Widget::getData).collect((Collector<? super Object, ?, List<Object>>)Collectors.toList());
        this.overviewTree.removeAll();
        final List<TreeItem> newSelection = new ArrayList<TreeItem>(selectedObjects.size());
        for (final EntityProperties entityProperties : this.mockDataSettings.getEntityPropertiesList()) {
            final DBSEntity entity = entityProperties.getEntity();
            final TreeItem entityItem = new TreeItem(this.overviewTree, 0);
            entityItem.setData((Object)entity);
            entityItem.setImage(DBeaverIcons.getImage(entity.getEntityType().getIcon()));
            entityItem.setText(0, DBUtils.getObjectFullName(entity.getDataSource(), (DBPNamedObject)entity, DBPEvaluationContext.UI));
            for (final DBSAttributeBase attribute : entityProperties.getAttributes()) {
                final TreeItem attributeItem = new TreeItem(entityItem, 0);
                attributeItem.setData((Object)attribute);
                attributeItem.setImage(DBeaverIcons.getImage(DBValueFormatting.getTypeImage((DBSTypedObject)attribute)));
                attributeItem.setText(0, attribute.getName().trim());
                attributeItem.setText(1, this.getAttributeGeneratorLabel(entity, attribute));
                if (selectedObjects.contains(attribute)) {
                    newSelection.add(attributeItem);
                }
            }
            entityItem.setExpanded(expandedEntities.isEmpty() || expandedEntities.contains(entity));
            if (selectedObjects.contains(entity)) {
                newSelection.add(entityItem);
            }
        }
        if (newSelection.isEmpty()) {
            final TreeItem[] roots = this.overviewTree.getItems();
            if (roots.length > 0) {
                newSelection.add(roots[0]);
                this.selectedEntity = (DBSEntity)roots[0].getData();
            }
        }
        this.overviewTree.setSelection((TreeItem[])newSelection.toArray(new TreeItem[0]));
        UIUtils.asyncExec(() -> UIUtils.packColumns(this.overviewTree, true, new float[] { 0.35f, 0.65f }));
    }
    
    private String getAttributeGeneratorLabel(@NotNull final DBSEntity entity, @NotNull final DBSAttributeBase attribute) {
        final EntityProperties entityProperties = this.mockDataSettings.getEntityProperties(entity);
        if (entityProperties == null) {
            return MockDataSettings.NO_GENERATOR_LABEL;
        }
        final EntityProperties.AttributeProperties attributeProperties = entityProperties.getAttributeProperties(attribute);
        if (attributeProperties == null || attributeProperties.isEmpty()) {
            return MockDataSettings.NO_GENERATOR_LABEL;
        }
        final MockGeneratorDescriptor generatorDescriptor = attributeProperties.getSelectedGenerator();
        if (generatorDescriptor == null) {
            return MockDataSettings.NO_GENERATOR_LABEL;
        }
        return generatorDescriptor.getLabel().trim();
    }
    
    public void activatePage() {
        if (this.firstInit) {
            try {
                UIUtils.run((IRunnableContext)this.getContainer(), true, true, monitor -> {
                    try {
                        this.firstInit = false;
                        this.mockDataSettings.init(monitor);
                        ((MockDataExecuteWizard)this.getWizard()).loadSettings(monitor);
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException((Throwable)e);
                    }
                });
            }
            catch (InvocationTargetException e) {
                MockDataWizardPageSettings.log.warn((Object)"Unable to initialize mock data settings");
                DBWorkbench.getPlatformUI().showError(MockDataUIMessages.tools_mockdata_wizard_page_settings_error_when_loading_settings_title, MockDataUIMessages.tools_mockdata_wizard_page_settings_error_when_loading_settings_message, e.getCause());
                this.setErrorMessage(MockDataUIMessages.tools_mockdata_wizard_page_settings_error_when_loading_settings_wizard_error_message);
            }
            catch (InterruptedException e2) {
                MockDataWizardPageSettings.log.error((Object)"Mock Data Settings initialization interrupted", (Throwable)e2);
            }
            this.loadingSettings = true;
            try {
                final EntityProperties entityProperties = this.mockDataSettings.getEntityPropertiesList().isEmpty() ? null : this.mockDataSettings.getEntityPropertiesList().get(0);
                if (entityProperties != null) {
                    this.updateGeneralSettings(entityProperties);
                }
                this.refreshOverviewTree();
            }
            finally {
                this.loadingSettings = false;
            }
            this.loadingSettings = false;
        }
        final EntityProperties entityProperties = this.mockDataSettings.getEntityPropertiesList().isEmpty() ? null : this.mockDataSettings.getEntityPropertiesList().get(0);
        if (entityProperties != null) {
            this.entityNameText.setText(getGeneralSettingsDisplayText(entityProperties));
            this.propertiesEditor.getControl().setFocus();
        }
        if (this.overviewTree.getItemCount() > 0) {
            final String entityName = this.mockDataSettings.getSelectedEntityName();
            final String selectedAttributeName = (entityProperties == null) ? "" : entityProperties.getSelectedAttribute();
            TreeItem item;
            if (selectedAttributeName != null) {
                item = this.findAttributeItem(entityName, selectedAttributeName);
                if (item == null) {
                    item = this.findEntityItem(entityName);
                }
            }
            else {
                item = this.findEntityItem(entityName);
            }
            if (item == null) {
                item = this.overviewTree.getItem(0);
            }
            this.overviewTree.select(item);
            this.overviewTree.showItem(item);
            final Event event = new Event();
            event.widget = (Widget)this.overviewTree;
            event.display = this.overviewTree.getDisplay();
            event.item = (Widget)item;
            event.type = 13;
            this.overviewTree.notifyListeners(13, event);
            this.setMessage((String)null);
        }
        else {
            this.setMessage(MockDataUIMessages.tools_mockdata_wizard_page_settings_button_info_noattributes, 1);
        }
        this.updatePageCompletion();
    }
    
    @NotNull
    private static String getGeneralSettingsDisplayText(@NotNull final EntityProperties entityProperties) {
        return DBUtils.getObjectFullName((DBPNamedObject)entityProperties.getEntity(), DBPEvaluationContext.DML);
    }
    
    private void updateGeneralSettings(@NotNull final DBSEntity entity) {
        final EntityProperties entityProperties = this.mockDataSettings.getEntityProperties(entity);
        if (entityProperties == null) {
            MockDataWizardPageSettings.log.debug((Object)("Unable to update general settings for entity " + entity + ": entity properties not found"));
            return;
        }
        this.updateGeneralSettings(entityProperties);
    }
    
    private void updateGeneralSettings(@NotNull final EntityProperties entityProperties) {
        this.entityNameText.setText(getGeneralSettingsDisplayText(entityProperties));
        this.removeOldDataCheck.setSelection(entityProperties.isRemoveOldData());
        this.rowsText.setText(String.valueOf(entityProperties.getRowsNumber()));
        this.batchSizeText.setText(String.valueOf(entityProperties.getBatchSize()));
    }
    
    @Nullable
    private TreeItem findEntityItem(final String entityName) {
        TreeItem[] items;
        for (int length = (items = this.overviewTree.getItems()).length, i = 0; i < length; ++i) {
            final TreeItem item = items[i];
            if (item.getText().equals(entityName)) {
                return item;
            }
        }
        return null;
    }
    
    @Nullable
    private TreeItem findAttributeItem(final String entityName, final String attributeName) {
        final TreeItem entityItem = this.findEntityItem(entityName);
        if (entityItem == null) {
            return null;
        }
        TreeItem[] items;
        for (int length = (items = entityItem.getItems()).length, i = 0; i < length; ++i) {
            final TreeItem item = items[i];
            if (item.getText().equals(attributeName)) {
                return item;
            }
        }
        return null;
    }
    
    public void deactivatePage() {
        this.saveGeneratorProperties();
    }
    
    public boolean isPageComplete() {
        return true;
    }
    
    boolean validateProperties() {
        for (final EntityProperties entityProperties : this.mockDataSettings.getEntityPropertiesList()) {
            for (final EntityProperties.AttributeProperties attributeGeneratorProperties : entityProperties.getAttributeGenerators().values()) {
                final PropertySourceCustom generatorProperties = attributeGeneratorProperties.getGeneratorProperties();
                if (generatorProperties == null) {
                    continue;
                }
                for (final Object value : generatorProperties.getPropertiesWithDefaults().values()) {
                    if (value instanceof Integer && (int)value < 0) {
                        return false;
                    }
                    if (value instanceof Long && (long)value < 0L) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private void updateState(@NotNull final DBSEntity entity) {
        if (this.loadingSettings) {
            return;
        }
        final EntityProperties properties = this.mockDataSettings.getEntityProperties(entity);
        if (properties == null) {
            MockDataWizardPageSettings.log.debug((Object)("Unable to update properties state for entity " + entity));
            return;
        }
        properties.setRemoveOldData(this.removeOldDataCheck.getSelection());
        properties.setRowsNumber(CommonUtils.toLong((Object)this.rowsText.getText()));
        properties.setBatchSize(CommonUtils.toInt((Object)this.batchSizeText.getText()));
    }
    
    private void reloadProperties(@NotNull final DBSEntity entity, @NotNull final DBSAttributeBase attribute, final String generatorId) {
        final EntityProperties entityProperties = this.mockDataSettings.getEntityProperties(entity);
        if (entityProperties == null) {
            MockDataWizardPageSettings.log.debug((Object)("Unable to reload properties for attribute " + attribute + ": properties for entity " + entity + "not found"));
            return;
        }
        final EntityProperties.AttributeProperties attributeProperties = entityProperties.getAttributeProperties(attribute);
        final MockGeneratorDescriptor newGenerator = (generatorId == null || MockDataSettings.NO_GENERATOR_LABEL.equals(generatorId)) ? null : attributeProperties.getGenerator(generatorId);
        attributeProperties.setSelectedGenerator(newGenerator);
        this.selectedEntity = entity;
        this.selectedAttribute = attribute;
        this.mockDataSettings.setSelectedEntityName(entity.getName());
        entityProperties.setSelectedAttribute(attribute.getName());
        this.propertySource = attributeProperties.getGeneratorProperties();
        if (this.propertySource == null) {
            this.propertiesEditor.clearProperties();
        }
        else {
            this.propertiesEditor.loadProperties((DBPPropertySource)this.propertySource);
            this.propertiesEditor.setExpandMode(PropertyTreeViewer.ExpandMode.FIRST);
            this.propertiesEditor.expandAll();
        }
        this.setPropertiesEditorColumnsWidth();
        final List<String> generators = new ArrayList<String>();
        generators.add(MockDataSettings.NO_GENERATOR_LABEL);
        for (final String genId : attributeProperties.getGenerators()) {
            final MockGeneratorDescriptor generatorDescriptor = entityProperties.getGeneratorDescriptor(genId);
            if (generatorDescriptor != null) {
                generators.add(generatorDescriptor.getLabel());
            }
        }
        if (!generators.isEmpty()) {
            this.generatorCombo.setItems((String[])generators.toArray(new String[0]));
            final MockGeneratorDescriptor generatorDescriptor2 = entityProperties.getGeneratorDescriptor(generatorId);
            if (generatorDescriptor2 != null) {
                this.generatorCombo.setText(generatorDescriptor2.getLabel());
            }
            else {
                this.generatorCombo.setText(MockDataSettings.NO_GENERATOR_LABEL);
            }
            this.generatorCombo.setEnabled(true);
        }
        else {
            this.generatorCombo.setItems(new String[] { MockDataSettings.NO_GENERATOR_LABEL, MockDataUIMessages.tools_mockdata_wizard_page_settings_notfound });
            this.generatorCombo.setText(MockDataUIMessages.tools_mockdata_wizard_page_settings_notfound);
            this.generatorCombo.setEnabled(false);
        }
        this.generatorCombo.getParent().layout();
    }
    
    private void setPropertiesEditorColumnsWidth() {
        UIUtils.asyncExec(() -> {
            ((Tree)this.propertiesEditor.getControl()).getColumn(0).setWidth(110);
            ((Tree)this.propertiesEditor.getControl()).getColumn(1).setWidth(this.propertiesEditor.getControl().getSize().x - 110 - 30);
        });
    }
    
    private void saveGeneratorProperties() {
        if (this.propertiesEditor != null) {
            this.propertiesEditor.saveEditorValues();
        }
        if (this.selectedEntity == null || this.selectedAttribute == null) {
            return;
        }
        final EntityProperties entityProperties = this.mockDataSettings.getEntityProperties(this.selectedEntity);
        if (entityProperties == null) {
            return;
        }
        final EntityProperties.AttributeProperties attributeProperties = entityProperties.getAttributeProperties(this.selectedAttribute);
        final MockGeneratorDescriptor selectedGenerator = attributeProperties.getSelectedGenerator();
        if (selectedGenerator == null) {
            return;
        }
        attributeProperties.putGeneratorProperties(selectedGenerator.getId(), this.propertySource);
    }
    
    static /* synthetic */ void access$11(final MockDataWizardPageSettings mockDataWizardPageSettings, final DBSEntity selectedEntity) {
        mockDataWizardPageSettings.selectedEntity = selectedEntity;
    }
    
    static /* synthetic */ void access$12(final MockDataWizardPageSettings mockDataWizardPageSettings, final DBSAttributeBase selectedAttribute) {
        mockDataWizardPageSettings.selectedAttribute = selectedAttribute;
    }
    
    static /* synthetic */ void access$13(final MockDataWizardPageSettings mockDataWizardPageSettings, final PropertySourceCustom propertySource) {
        mockDataWizardPageSettings.propertySource = propertySource;
    }
    
    private static class ShowSummaryDialog extends BaseDialog
    {
        private static final String INDENT = "  ";
        private static final String DOUBLE_INDENT = "    ";
        private final String text;
        
        private ShowSummaryDialog(final Shell parentShell, final MockDataSettings mockDataSettings) {
            super(parentShell, MockDataUIMessages.tools_mockdata_wizard_page_settings_dialog_summary_title, (DBPImage)null);
            this.text = buildSummary(mockDataSettings);
        }
        
        protected void createButtonsForButtonBar(final Composite parent) {
            this.createButton(parent, 12, IDialogConstants.CLOSE_LABEL, true);
        }
        
        protected void buttonPressed(final int buttonId) {
            this.setReturnCode(12);
            this.close();
        }
        
        private static String buildSummary(final MockDataSettings mockDataSettings) {
            final StringBuilder builder = new StringBuilder();
            for (final EntityProperties entityProperties : mockDataSettings.getEntityPropertiesList()) {
                builder.append("Entity: ").append(entityProperties.getInputObject().getName()).append("\n");
                builder.append("  ").append("General settings\n");
                builder.append("    ").append("Remove old data: ").append(entityProperties.isRemoveOldData() ? "yes" : "no").append("\n");
                builder.append("    ").append("Row count: ").append(entityProperties.getRowsNumber()).append("\n");
                builder.append("    ").append("Batch size: ").append(entityProperties.getBatchSize()).append("\n");
                for (final DBSAttributeBase attribute : entityProperties.getAttributes()) {
                    builder.append("  ").append("Attribute: ").append(attribute.getName().trim()).append("\n");
                    final EntityProperties.AttributeProperties properties = entityProperties.getAttributeProperties(attribute);
                    final MockGeneratorDescriptor descriptor = properties.getSelectedGenerator();
                    builder.append("    ");
                    if (descriptor == null) {
                        builder.append("No generator selected\n");
                    }
                    else {
                        builder.append("Generator: ").append(descriptor.getLabel().trim()).append("\n");
                        final Map<String, Object> propertyMap = (Map<String, Object>)properties.getGeneratorProperties().getPropertiesWithDefaults();
                        for (final DBPPropertyDescriptor propertyDescriptor : descriptor.getProperties()) {
                            final Object propertyValue = propertyMap.get(propertyDescriptor.getId());
                            if (propertyValue == null) {
                                MockDataWizardPageSettings.log.debug((Object)"Unexpected empty property value while building mock data settings summary");
                            }
                            else {
                                builder.append("    ").append(propertyDescriptor.getDisplayName()).append(": ").append(propertyValue).append("\n");
                            }
                        }
                    }
                }
            }
            return builder.toString();
        }
        
        protected Composite createDialogArea(final Composite parent) {
            final Composite dialogComposite = super.createDialogArea(parent);
            final Text textWidget = new Text(dialogComposite, 2568);
            textWidget.setLayoutData((Object)new GridData(1808));
            textWidget.setText(this.text);
            textWidget.setFont(UIUtils.getMonospaceFont());
            return dialogComposite;
        }
    }
}
