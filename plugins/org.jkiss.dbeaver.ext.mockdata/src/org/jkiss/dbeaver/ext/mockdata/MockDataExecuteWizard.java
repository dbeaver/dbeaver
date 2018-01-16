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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizard;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class MockDataExecuteWizard  extends AbstractToolWizard<DBSDataManipulator, DBSDataManipulator> implements IImportWizard{

    private static final Log log = Log.getLog(MockDataExecuteWizard.class);

    private MockDataWizardPageSettings settingsPage;

    boolean removeOldData;

    public MockDataExecuteWizard(Collection<DBSDataManipulator> dbObjects, String task) {
        super(dbObjects, task);
        this.clientHomeRequired = false;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(task);
        setNeedsProgressMonitor(true);
        settingsPage = new MockDataWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addPage(settingsPage);
        addPage(logPage);
        super.addPages();
    }

    @Override
    public void onSuccess(long workTime) {
        UIUtils.showMessageBox(
                getShell(),
                MockDataMessages.tools_mockdata_wizard_page_name,
                CommonUtils.truncateString(NLS.bind(MockDataMessages.tools_mockdata_wizard_message_process_completed, getObjectsName()), 255),
                SWT.ICON_INFORMATION);
    }

    public DBPClientHome findServerHome(String clientHomeId) {
        return null;
    }

    @Override
    public Collection<DBSDataManipulator> getRunInfo() {
        return getDatabaseObjects();
    }

    protected List<String> getCommandLine(DBSDataManipulator jdbcTable) throws IOException {
        return null;
    }

    public void fillProcessParameters(List<String> cmd, DBSDataManipulator jdbcTable) throws IOException {

    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBSDataManipulator dbsDataManipulator, ProcessBuilder processBuilder, Process process) {
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);
    }

    @Override
    public boolean executeProcess(DBRProgressMonitor monitor, DBSDataManipulator dataManipulator) {

        DBCExecutionContext context = dataManipulator.getDataSource().getDefaultContext(true);
        DBCSession session = context.openSession(monitor, DBCExecutionPurpose.USER, MockDataMessages.tools_mockdata_generate_data_task);
        AbstractExecutionSource executionSource = new AbstractExecutionSource(dataManipulator, session.getExecutionContext(), this);
        try {
            if (removeOldData) {
                logPage.appendLog("Removing old data from the '" + dataManipulator.getName() + "'.\n");
                DBCStatistics deleteStats = new DBCStatistics();
                try {
                    DBSDataManipulator.ExecuteBatch batch = dataManipulator.deleteData(session, new DBSAttributeBase[]{}, executionSource);
                    try {
                        batch.add(new Object[] {});
                        deleteStats.accumulate(batch.execute(session));
                    } finally {
                        batch.close();
                    }
                } catch (Exception e) {
                    String message = "Error removing data from the '" + dataManipulator.getName() + "'.";
                    log.error(message, e);
                    logPage.appendLog(message, true);
                } finally {
                    monitor.done();
                }
                logPage.appendLog("    Rows updated: " + deleteStats.getRowsUpdated() + "\n");
                logPage.appendLog("    Duration: " + deleteStats.getExecuteTime() + "ms\n");
            } else {
                logPage.appendLog("Old data isn't removed.\n");
            }

            try {
                DBCStatistics insertStats = new DBCStatistics();

                List<DBDAttributeValue> keyAttributes = new ArrayList<>();
                Collection<? extends DBSEntityAttribute> attributes = ((DBSEntity) dataManipulator).getAttributes(monitor);
                Random random = new Random();
                for (DBSEntityAttribute attribute : attributes) {
                    Object value = attribute.getDefaultValue();
                    DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
                    DBPDataKind dataKind = dataType.getDataKind();
                    switch (dataKind) {
                        case NUMERIC:
                            value = MockDataGenerator.generateNumeric((int) attribute.getMaxLength(), attribute.getPrecision(), attribute.getScale()); break;
                        case STRING:
                            value = MockDataGenerator.generateTextUpTo((int) attribute.getMaxLength()); break;
                        case DATETIME:
                            value = MockDataGenerator.generateDate(); break;
                    }
                    keyAttributes.add(new DBDAttributeValue(attribute, value));
                }

                try (DBSDataManipulator.ExecuteBatch batch = dataManipulator.insertData(
                        session,
                        DBDAttributeValue.getAttributes(keyAttributes),
                        null,
                        executionSource))
                {
                    batch.add(DBDAttributeValue.getValues(keyAttributes));
                    insertStats.accumulate(batch.execute(session));
                }
            } catch (DBException e) {
                e.printStackTrace();
            }

        } finally {
            session.close();
        }

        // TODO generate and insert the mock data to the table
        logPage.appendLog("\nGenerate and insert the mock data into the table '" + dataManipulator.getName() + "' isn't implemented yet.\n\n");

        return true;
    }
}
