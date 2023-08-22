// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui;

import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;
import java.io.IOException;
import java.util.Set;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.Collections;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import java.util.List;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import java.util.HashSet;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import java.util.ArrayList;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.jface.wizard.IWizardPage;
import java.lang.reflect.InvocationTargetException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import java.util.Iterator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.ui.UIUtils;
import java.util.HashMap;
import org.jkiss.dbeaver.model.task.DBTTask;
import java.util.Map;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;
import org.jkiss.dbeaver.mockdata.engine.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.mockdata.engine.model.MockValueGenerator;
import org.jkiss.dbeaver.mockdata.ui.internal.MockDataUIActivator;
import org.jkiss.dbeaver.mockdata.ui.internal.MockDataUIMessages;
import org.eclipse.ui.IImportWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

public class MockDataExecuteWizard extends TaskConfigurationWizard<MockDataSettings> implements IImportWizard
{
    private static final Log log;
    private static final String WIZARD_DIALOG_SETTINGS = "MockData";
    private static final boolean JUST_GENERATE_SCRIPT = false;
    private final Map<String, MockValueGenerator> generators;
    private final MockDataSettings mockDataSettings;
    private MockDataWizardPageSettings settingsPage;
    private MockDataWizardPageLog logPage;
    
    static {
        log = Log.getLog(MockDataExecuteWizard.class);
    }
    
    MockDataExecuteWizard(final MockDataSettings mockDataSettings) {
        super((DBTTask)null);
        this.generators = new HashMap<String, MockValueGenerator>();
        this.mockDataSettings = mockDataSettings;
        this.setDialogSettings(UIUtils.getSettingsSection(MockDataUIActivator.getDefault().getDialogSettings(), "MockData"));
    }
    
    protected MockDataSettings getSettings() {
        return this.mockDataSettings;
    }
    
    protected String getDefaultWindowTitle() {
        return MockDataUIMessages.tools_mockdata_wizard_title;
    }
    
    public String getTaskTypeId() {
        return "mockDataGenerate";
    }
    
    public void saveTaskState(final DBRRunnableContext runnableContext, final DBTTask task, final Map<String, Object> state) {
    }
    
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        this.setNeedsProgressMonitor(true);
        this.settingsPage = new MockDataWizardPageSettings(this.mockDataSettings);
        this.logPage = new MockDataWizardPageLog(this.getDefaultWindowTitle());
        this.updateWizardTitle();
    }
    
    void loadSettings(final DBRProgressMonitor monitor) {
        this.mockDataSettings.loadFrom(monitor, this.getDialogSettings());
    }
    
    public boolean canFinish() {
        if (!super.canFinish() || !this.mockDataSettings.isInitialized()) {
            return false;
        }
        for (final EntityProperties properties : this.mockDataSettings.getEntityPropertiesList()) {
            if (properties.getAttributeGenerators().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    public boolean performCancel() {
        this.mockDataSettings.saveTo(this.getDialogSettings());
        return super.performCancel();
    }
    
    public boolean performFinish() {
        this.mockDataSettings.saveTo(this.getDialogSettings());
        this.showLogPage();
        try {
            this.getContainer().run(true, true, monitor1 -> {
                final DBRProgressMonitor monitor2 = (DBRProgressMonitor)new DefaultProgressMonitor(monitor1);
                this.mockDataSettings.sortEntityProperties(monitor2);
                try {
                    monitor2.beginTask("Generating mock data", this.mockDataSettings.getEntityPropertiesList().size());
                    for (final EntityProperties entityProperties : this.mockDataSettings.getEntityPropertiesList()) {
                        this.generateMockData(monitor2, entityProperties);
                    }
                }
                catch (IOException e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
        catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Mock data generator", "Mock data generation failed", (Throwable)e);
        }
        catch (InterruptedException ex) {}
        return false;
    }
    
    private void showLogPage() {
        if (this.getContainer().getCurrentPage() != this.logPage) {
            this.getContainer().showPage((IWizardPage)this.logPage);
        }
    }
    
    public void addPages() {
        this.addPage((IWizardPage)this.settingsPage);
        this.addPage((IWizardPage)this.logPage);
        super.addPages();
    }
    
    public void createPageControls(final Composite pageContainer) {
        super.createPageControls(pageContainer);
    }
    
    public boolean isRunTaskOnFinish() {
        return false;
    }
    
    public boolean generateMockData(final DBRProgressMonitor monitor, final EntityProperties entityProperties) throws IOException {
        final DBSDataManipulator dataManipulator = entityProperties.getDataManipulator();
        final DBCExecutionContext context = DBUtils.getDefaultContext((DBSObject)dataManipulator, false);
        try {
            Throwable t = null;
            try {
                final DBCSession session = context.openSession(monitor, DBCExecutionPurpose.USER, MockDataUIMessages.tools_mockdata_wizard_task_generate_data);
                try {
                    final DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
                    boolean autoCommit;
                    try {
                        autoCommit = (txnManager == null || txnManager.isAutoCommit());
                    }
                    catch (DBCException e) {
                        MockDataExecuteWizard.log.error(e);
                        autoCommit = true;
                    }
                    final AbstractExecutionSource executionSource = new AbstractExecutionSource((DBSDataContainer)dataManipulator, session.getExecutionContext(), (Object)this);
                    boolean success = true;
                    monitor.beginTask(MockDataUIMessages.tools_mockdata_wizard_task_generate_data, 3);
                    final ArrayList<DBEPersistAction> persistActions = new ArrayList<DBEPersistAction>();
                    if (entityProperties.isRemoveOldData()) {
                        this.logPage.appendLog(NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_removing_from, (Object)dataManipulator.getName()));
                        monitor.subTask(MockDataUIMessages.tools_mockdata_wizard_log_cleaning);
                        final DBCStatistics deleteStats = new DBCStatistics();
                        try {
                            dataManipulator.truncateData(session, (DBCExecutionSource)executionSource);
                            if (txnManager != null && txnManager.isSupportsTransactions() && !autoCommit) {
                                txnManager.commit(session);
                            }
                        }
                        catch (Exception e2) {
                            success = false;
                            final String message = String.valueOf(MockDataUIMessages.tools_mockdata_wizard_log_removing_error) + "\n" + e2.getMessage();
                            MockDataExecuteWizard.log.error((Object)message, (Throwable)e2);
                            this.logPage.appendLog(String.valueOf(message) + "\n\n", true);
                        }
                        this.logPage.appendLog(NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_duration, (Object)deleteStats.getExecuteTime()));
                    }
                    else {
                        this.logPage.appendLog(MockDataUIMessages.tools_mockdata_wizard_log_not_removed);
                    }
                    if (!success) {
                        return true;
                    }
                    try {
                        monitor.subTask(MockDataUIMessages.tools_mockdata_wizard_task_insert_data);
                        this.logPage.appendLog(NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_inserting_into, (Object)dataManipulator.getName()));
                        final DBCStatistics insertStats = new DBCStatistics();
                        this.generators.clear();
                        final DBSEntity dbsEntity = (DBSEntity)dataManipulator;
                        final List<DBSAttributeBase> attributes = new ArrayList<DBSAttributeBase>();
                        for (final EntityProperties.AttributeProperties attributeProps : entityProperties.getAttributeGenerators().values()) {
                            final MockGeneratorDescriptor attrGenerator = attributeProps.getSelectedGenerator();
                            if (attrGenerator != null) {
                                final MockValueGenerator generatorInstance = attrGenerator.createGenerator();
                                final DBSAttributeBase attribute = attributeProps.getAttribute();
                                final EntityProperties.AttributeProperties generatorPropertySource = entityProperties.getAttributeProperties(attribute);
                                final PropertySourceCustom generatorProperties = generatorPropertySource.getGeneratorProperties();
                                if (generatorProperties != null) {
                                    final Map<String, Object> propValues = (Map<String, Object>)generatorProperties.getPropertiesWithDefaults();
                                    generatorInstance.init(dataManipulator, attribute, (Map)propValues);
                                    this.generators.put(attribute.getName(), generatorInstance);
                                }
                                attributes.add(attribute);
                            }
                        }
                        monitor.done();
                        final long rowsNumber = entityProperties.getRowsNumber();
                        int batchSize = entityProperties.getBatchSize();
                        if (batchSize <= 0) {
                            batchSize = 1;
                        }
                        long quotient = rowsNumber / batchSize;
                        final long modulo = rowsNumber % batchSize;
                        if (modulo > 0L) {
                            ++quotient;
                        }
                        int counter = 0;
                        monitor.beginTask(MockDataUIMessages.tools_mockdata_wizard_task_insert_data, (int)rowsNumber);
                        boolean hasMiltiUniqs = false;
                        final Set<String> miltiUniqColumns = new HashSet<String>();
                        for (final DBSAttributeBase attribute2 : attributes) {
                            if (MockDataUtils.checkUnique(monitor, dbsEntity, attribute2) != MockDataUtils.UNIQ_TYPE.MULTI) {
                                continue;
                            }
                            hasMiltiUniqs = true;
                            final DBSEntityReferrer constraint = (DBSEntityReferrer)DBUtils.getConstraint(monitor, dbsEntity, attribute2);
                            for (final DBSEntityAttributeRef attributeRef : constraint.getAttributeReferences(monitor)) {
                                miltiUniqColumns.add(attributeRef.getAttribute().getName());
                            }
                        }
                        final List<List<DBDAttributeValue>> valuesCacheForUniqs = new ArrayList<List<DBDAttributeValue>>();
                        session.enableLogging(false);
                        DBSDataManipulator.ExecuteBatch batch = null;
                        for (int q = 0; q < quotient && !monitor.isCanceled(); ++q) {
                            if (counter > 0) {
                                if (txnManager != null && txnManager.isSupportsTransactions() && !autoCommit) {
                                    txnManager.commit(session);
                                }
                                monitor.subTask(NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_inserted_rows, (Object)String.valueOf(counter)));
                            }
                            try {
                                for (int i = 0; i < batchSize && counter < rowsNumber && !monitor.isCanceled(); ++i) {
                                    final List<DBDAttributeValue> attributeValues = new ArrayList<DBDAttributeValue>();
                                    try {
                                        for (final DBSAttributeBase attribute3 : attributes) {
                                            final MockValueGenerator generator = this.generators.get(attribute3.getName());
                                            if (generator != null) {
                                                final Object value = generator.generateValue(monitor);
                                                attributeValues.add(new DBDAttributeValue(attribute3, value));
                                            }
                                        }
                                    }
                                    catch (DBException e3) {
                                        this.processGeneratorException((Exception)e3);
                                        return true;
                                    }
                                    if (hasMiltiUniqs) {
                                        boolean collision = false;
                                        for (final List<DBDAttributeValue> valueList : valuesCacheForUniqs) {
                                            boolean theSame = true;
                                            for (int j = 0; j < valueList.size(); ++j) {
                                                if (miltiUniqColumns.contains(valueList.get(j).getAttribute().getName()) && !CommonUtils.equalObjects((Object)valueList.get(j), (Object)attributeValues.get(j))) {
                                                    theSame = false;
                                                    break;
                                                }
                                            }
                                            if (theSame) {
                                                collision = true;
                                                break;
                                            }
                                        }
                                        if (collision) {
                                            continue;
                                        }
                                        valuesCacheForUniqs.add(attributeValues);
                                    }
                                    if (batch == null) {
                                        batch = dataManipulator.insertData(session, DBDAttributeValue.getAttributes(attributeValues), (DBDDataReceiver)null, (DBCExecutionSource)executionSource,new HashMap<>());
                                    }
                                    if (counter++ < rowsNumber) {
                                        batch.add(DBDAttributeValue.getValues(attributeValues));
                                    }
                                    monitor.worked(1);
                                }
                                if (batch != null) {
                                    insertStats.accumulate(batch.execute(session, Collections.emptyMap()));
                                }
                            }
                            catch (Exception e4) {
                                this.processGeneratorException(e4);
                                if (e4 instanceof DBException) {
                                    throw e4;
                                }
                                continue;
                            }
                            finally {
                                if (batch != null) {
                                    batch.close();
                                    batch = null;
                                }
                            }
                            if (batch != null) {
                                batch.close();
                                batch = null;
                            }
                        }
                        if (txnManager != null && txnManager.isSupportsTransactions() && !autoCommit) {
                            txnManager.commit(session);
                        }
                        this.logPage.appendLog(NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_rows_updated, (Object)insertStats.getRowsUpdated()));
                        this.logPage.appendLog(NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_duration, (Object)insertStats.getExecuteTime()));
                    }
                    catch (DBException e5) {
                        final String message2 = NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_error_inserting, (Object)e5.getMessage());
                        MockDataExecuteWizard.log.error((Object)message2, (Throwable)e5);
                        this.logPage.appendLog(String.valueOf(message2) + "\n\n", true);
                    }
                }
                finally {
                    if (session != null) {
                        session.close();
                    }
                }
            }
            finally {
            	
            }
        }
        finally {
            monitor.done();
        }
        monitor.done();
        return true;
    }
    
    private void processGeneratorException(final Exception e) {
        final String message = NLS.bind(MockDataUIMessages.tools_mockdata_wizard_log_error_generating, (Object)e.getMessage());
        MockDataExecuteWizard.log.error(message, e);
        this.logPage.appendLog(String.valueOf(message) + "\n\n", true);
    }
    
    public IWizardPage getNextPage(final IWizardPage page) {
        return null;
    }
}
