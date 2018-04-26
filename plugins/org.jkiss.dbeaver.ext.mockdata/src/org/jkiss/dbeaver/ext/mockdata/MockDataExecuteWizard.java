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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.ext.mockdata.model.MockValueGenerator;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.*;

public class MockDataExecuteWizard  extends AbstractToolWizard<DBSDataManipulator, DBSDataManipulator> implements IImportWizard
{
    private static final Log log = Log.getLog(MockDataExecuteWizard.class);

    private static final int BATCH_SIZE = 1000;
    public static final boolean JUST_GENERATE_SCRIPT = false;

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "MockData"; //$NON-NLS-1$

    private MockDataWizardPageSettings settingsPage;
    private MockDataSettings mockDataSettings;

    MockDataExecuteWizard(MockDataSettings mockDataSettings, Collection<DBSDataManipulator> dbObjects, String task) {
        super(dbObjects, task);
        this.clientHomeRequired = false;
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

    public DBPClientHome findServerHome(String clientHomeId) {
        return null;
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

        DBCExecutionContext context = dataManipulator.getDataSource().getDefaultContext(true);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.USER, MockDataMessages.tools_mockdata_generate_data_task)) {
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
            monitor.beginTask("Generate Mock Data", 3);
            ArrayList<DBEPersistAction> persistActions = new ArrayList<>();
            if (mockDataSettings.isRemoveOldData()) {
                logPage.appendLog("Removing old data from the '" + dataManipulator.getName() + "'.\n");
                monitor.subTask("Cleanup old data");
                DBCStatistics deleteStats = new DBCStatistics();
                try {
                    dataManipulator.truncateData(session, executionSource);
                    if (txnManager != null && !autoCommit) {
                        txnManager.commit(session);
                    }
                } catch (Exception e) {
                    success = false;
                    String message = "    Error removing the data: " + e.getMessage();
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
                logPage.appendLog("    Rows updated: " + deleteStats.getRowsUpdated() + "\n");
                logPage.appendLog("    Duration: " + deleteStats.getExecuteTime() + "ms\n\n");
            } else {
                logPage.appendLog("Old data isn't removed.\n\n");
            }

            if (!success) {
                return true;
            }

            try {
                monitor.subTask("Insert data");

                logPage.appendLog("Inserting mock data into the '" + dataManipulator.getName() + "'.\n");
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
                long quotient = rowsNumber / BATCH_SIZE;
                long modulo = rowsNumber % BATCH_SIZE;
                if (modulo > 0) {
                    quotient++;
                }
                int counter = 0;

                monitor.beginTask("Insert data", (int) rowsNumber);

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

                        monitor.subTask(String.valueOf(counter) + " rows inserted");
                        monitor.worked(BATCH_SIZE);
                    }

                    try {
                        for (int i = 0; (i < BATCH_SIZE && counter < rowsNumber); i++) {
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
                logPage.appendLog("    Rows updated: " + insertStats.getRowsUpdated() + "\n");
                logPage.appendLog("    Duration: " + insertStats.getExecuteTime() + "ms\n\n");

            } catch (DBException e) {
                String message = "    Error inserting mock data: " + e.getMessage();
                log.error(message, e);
                logPage.appendLog(message + "\n\n", true);
            }

        } finally {
            monitor.done();
        }

        return true;
    }

    private void processGeneratorException(Exception e) {
        String message = "    Error generating mock data: " + e.getMessage();
        log.error(message, e);
        logPage.appendLog(message + "\n\n", true);
    }
}
