/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mockdata;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.ext.mockdata.model.MockValueGenerator;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.*;

public class MockDataExecuteWizard  extends AbstractToolWizard<DBSDataManipulator, DBSDataManipulator> implements IImportWizard
{
    private static final Log log = Log.getLog(MockDataExecuteWizard.class);

    public static final boolean JUST_GENERATE_SCRIPT = false;

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "MockData"; //$NON-NLS-1$

    private MockDataWizardPageSettings settingsPage;
    private MockDataSettings mockDataSettings;

    MockDataExecuteWizard(MockDataSettings mockDataSettings, Collection<DBSDataManipulator> dbObjects, String task) {
        super(dbObjects, task);
        this.nativeClientHomeRequired = false;
        this.mockDataSettings = mockDataSettings;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(task);
        setNeedsProgressMonitor(true);

        settingsPage = new MockDataWizardPageSettings(mockDataSettings);

    }

    void loadSettings() {
        IDialogSettings section = UIUtils.getDialogSettings(RS_EXPORT_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        mockDataSettings.loadFrom(section);
    }

    @Override
    public boolean canFinish() {
        try {
            Collection<? extends DBSEntityAttribute> attributes =
                    mockDataSettings.getEntity().getAttributes(mockDataSettings.getMonitor());
            return super.canFinish() && !CommonUtils.isEmpty(DBUtils.getRealAttributes(attributes));
        } catch (DBException ex) {
            log.error("Error accessing DB entity " + mockDataSettings.getEntity().getName(), ex);
            return false;
        }
    }

    @Override
    public boolean performCancel() {
        // Save settings anyway
        mockDataSettings.saveTo(getDialogSettings());

        return super.performCancel();
    }

    @Override
    public boolean performFinish() {
        // Save settings
        mockDataSettings.saveTo(getDialogSettings());

        return super.performFinish();
    }

    @Override
    public void addPages() {
        addPage(settingsPage);
        addPage(logPage);
        super.addPages();
    }

    @Override
    public void onSuccess(long workTime) {
/*
        UIUtils.showMessageBox(
                getShell(),
                MockDataMessages.tools_mockdata_wizard_page_name,
                CommonUtils.truncateString(NLS.bind(MockDataMessages.tools_mockdata_wizard_message_process_completed, getObjectsName()), 255),
                SWT.ICON_INFORMATION);
*/
    }

    @Override
    public Collection<DBSDataManipulator> getRunInfo() {
        return getDatabaseObjects();
    }

    protected List<String> getCommandLine(DBSDataManipulator jdbcTable) {
        return null;
    }

    public void fillProcessParameters(List<String> cmd, DBSDataManipulator jdbcTable) {
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBSDataManipulator dbsDataManipulator, ProcessBuilder processBuilder, Process process) {
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);
    }

    private Map<String, MockValueGenerator> generators = new HashMap<>();

    @Override
    public boolean executeProcess(DBRProgressMonitor monitor, DBSDataManipulator dataManipulator) throws IOException {

        DBCExecutionContext context = DBUtils.getDefaultContext(dataManipulator, false);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.USER, MockDataMessages.tools_mockdata_wizard_task_generate_data)) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            boolean autoCommit;
            try {
                autoCommit = txnManager == null || txnManager.isAutoCommit();
            } catch (DBCException e) {
                log.error(e);
                autoCommit = true;
            }
            AbstractExecutionSource executionSource = new AbstractExecutionSource(dataManipulator, session.getExecutionContext(), this);

            boolean success = true;
            monitor.beginTask(MockDataMessages.tools_mockdata_wizard_task_generate_data, 3);
            ArrayList<DBEPersistAction> persistActions = new ArrayList<>();
            if (mockDataSettings.isRemoveOldData()) {
                logPage.appendLog(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_removing_from, dataManipulator.getName()));
                monitor.subTask(MockDataMessages.tools_mockdata_wizard_log_cleaning);
                DBCStatistics deleteStats = new DBCStatistics();
                try {
                    dataManipulator.truncateData(session, executionSource);
                    if (txnManager != null && !autoCommit) {
                        txnManager.commit(session);
                    }
                } catch (Exception e) {
                    success = false;
                    String message = NLS.bind(MockDataMessages.tools_mockdata_wizard_log_removing_error, e.getMessage());
                    log.error(message, e);
                    logPage.appendLog(message + "\n\n", true);
                }
                if (JUST_GENERATE_SCRIPT) {
                    String scriptText = SQLUtils.generateScript(
                            dataManipulator.getDataSource(),
                            persistActions.toArray(new DBEPersistAction[persistActions.size()]),
                            false);
                    logPage.appendLog("    The insert data script:\n " + scriptText + "\n\n");
                }
                // logPage.appendLog(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_rows_updated, deleteStats.getRowsUpdated())); no reason because trancate
                logPage.appendLog(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_duration, deleteStats.getExecuteTime()));
            } else {
                logPage.appendLog(MockDataMessages.tools_mockdata_wizard_log_not_removed);
            }

            if (!success) {
                return true;
            }

            try {
                monitor.subTask(MockDataMessages.tools_mockdata_wizard_task_insert_data);

                logPage.appendLog(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_inserting_into, dataManipulator.getName()));
                DBCStatistics insertStats = new DBCStatistics();
                persistActions = new ArrayList<>();

                // build and init the generators
                generators.clear();
                DBSEntity dbsEntity = (DBSEntity) dataManipulator;
                Collection<? extends DBSAttributeBase> attributes = DBUtils.getRealAttributes(dbsEntity.getAttributes(monitor));
                for (DBSAttributeBase attribute : attributes) {
                    MockGeneratorDescriptor generatorDescriptor = mockDataSettings.getGeneratorDescriptor(mockDataSettings.getAttributeGeneratorProperties(attribute).getSelectedGeneratorId());
                    if (generatorDescriptor != null) {
                        MockValueGenerator generator = generatorDescriptor.createGenerator();

                        MockDataSettings.AttributeGeneratorProperties generatorPropertySource = this.mockDataSettings.getAttributeGeneratorProperties(attribute);
                        String selectedGenerator = generatorPropertySource.getSelectedGeneratorId();
                        Map<Object, Object> generatorProperties =
                                generatorPropertySource.getGeneratorPropertySource(selectedGenerator).getPropertiesWithDefaults();
                        generator.init(dataManipulator, attribute, generatorProperties);
                        generators.put(attribute.getName(), generator);
                    }
                }

                monitor.done();

                long rowsNumber = mockDataSettings.getRowsNumber();
                int batchSize = mockDataSettings.getBatchSize();
                if (batchSize <= 0) {
                    batchSize = 1;
                }
                long quotient = rowsNumber / batchSize;
                long modulo = rowsNumber % batchSize;
                if (modulo > 0) {
                    quotient++;
                }
                int counter = 0;

                monitor.beginTask(MockDataMessages.tools_mockdata_wizard_task_insert_data, (int) rowsNumber);

                boolean hasMiltiUniqs = false;
                Set<String> miltiUniqColumns = new HashSet<>();
                for (DBSAttributeBase attribute : attributes) {
                    if (DBUtils.checkUnique(monitor, dbsEntity, attribute) == DBUtils.UNIQ_TYPE.MULTI) {
                        hasMiltiUniqs = true;

                        // collect the columns from multi-uniqs
                        DBSEntityReferrer constraint = (DBSEntityReferrer) DBUtils.getConstraint(monitor, dbsEntity, attribute);
                        for (DBSEntityAttributeRef attributeRef : constraint.getAttributeReferences(monitor)) {
                            miltiUniqColumns.add(attributeRef.getAttribute().getName());
                        }
                    }
                }
                List<List<DBDAttributeValue>> valuesCacheForUniqs = new ArrayList<>();

                // generate and insert the data
                session.enableLogging(false);
                DBSDataManipulator.ExecuteBatch batch = null;
                for (int q = 0; q < quotient; q++) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    if (counter > 0) {
                        if (txnManager != null && !autoCommit) {
                            txnManager.commit(session);
                        }

                        monitor.subTask(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_inserted_rows, String.valueOf(counter)));
                        //monitor.worked(batchSize);
                    }

                    try {
                        for (int i = 0; (i < batchSize && counter < rowsNumber); i++) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            List<DBDAttributeValue> attributeValues = new ArrayList<>();
                            try {
                                for (DBSAttributeBase attribute : attributes) {
                                    MockValueGenerator generator = generators.get(attribute.getName());
                                    if (generator != null) {
                                        //((AbstractMockValueGenerator) generator).checkUnique(monitor);
                                        Object value = generator.generateValue(monitor);
                                        attributeValues.add(new DBDAttributeValue(attribute, value));
                                    }
                                }
                            } catch (DBException e) {
                                processGeneratorException(e);
                                return true;
                            }

                            // skip duplicate records for uniqs
                            if (hasMiltiUniqs) {
                                boolean collision = false;
                                for (List<DBDAttributeValue> valueList : valuesCacheForUniqs) {
                                    boolean theSame = true;
                                    for (int j = 0; j < valueList.size(); j++) {
                                        if (miltiUniqColumns.contains(valueList.get(j).getAttribute().getName())) {
                                            if (!CommonUtils.equalObjects(valueList.get(j), attributeValues.get(j))) {
                                                theSame = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (theSame) {
                                        collision = true;
                                        break;
                                    }
                                }
                                if (collision) {
                                    continue;
                                } else {
                                    valuesCacheForUniqs.add(attributeValues);
                                }
                            }

                            if (batch == null) {
                                batch = dataManipulator.insertData(
                                        session,
                                        DBDAttributeValue.getAttributes(attributeValues),
                                        null,
                                        executionSource);
                            }
                            if (counter++ < rowsNumber) {
                                batch.add(DBDAttributeValue.getValues(attributeValues));
                            }
                            monitor.worked(1);
                        }
                        if (batch != null) {
                            if (JUST_GENERATE_SCRIPT) {
                                batch.generatePersistActions(session, persistActions);
                            } else {
                                insertStats.accumulate(batch.execute(session));
                            }
                        }
                    }
                    catch (Exception e) {
                        processGeneratorException(e);
                        if (e instanceof DBException) {
                            throw e;
                        }
                    }
                    finally {
                        if (batch != null) {
                            batch.close();
                            batch = null;
                        }
                    }
                }

                if (txnManager != null && !autoCommit) {
                    txnManager.commit(session);
                }

                if (JUST_GENERATE_SCRIPT) {
                    String scriptText = SQLUtils.generateScript(
                            dataManipulator.getDataSource(),
                            persistActions.toArray(new DBEPersistAction[persistActions.size()]),
                            false);
                    logPage.appendLog("    The insert data script:\n " + scriptText + "\n\n");
                }
                logPage.appendLog(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_rows_updated, insertStats.getRowsUpdated()));
                logPage.appendLog(NLS.bind(MockDataMessages.tools_mockdata_wizard_log_duration, insertStats.getExecuteTime()));

            } catch (DBException e) {
                String message = NLS.bind(MockDataMessages.tools_mockdata_wizard_log_error_inserting, e.getMessage());
                log.error(message, e);
                logPage.appendLog(message + "\n\n", true);
            }

        } finally {
            monitor.done();
        }

        return true;
    }

    private void processGeneratorException(Exception e) {
        String message = NLS.bind(MockDataMessages.tools_mockdata_wizard_log_error_generating, e.getMessage());
        log.error(message, e);
        logPage.appendLog(message + "\n\n", true);
    }
}
