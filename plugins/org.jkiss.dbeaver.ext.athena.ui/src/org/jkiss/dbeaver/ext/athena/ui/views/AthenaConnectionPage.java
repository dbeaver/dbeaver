/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.athena.ui.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.athena.model.AWSRegion;
import org.jkiss.dbeaver.ext.athena.model.AthenaConstants;
import org.jkiss.dbeaver.ext.athena.ui.AthenaActivator;
import org.jkiss.dbeaver.ext.athena.ui.internal.AthenaMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystem;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystems;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.registry.fs.FileSystemProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.dbeaver.ui.navigator.dialogs.ObjectBrowserDialogBase;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * AthenaConnectionPage
 */
public class AthenaConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Combo awsRegionCombo;
    private Text s3LocationText;

    private static final ImageDescriptor logoImage = AthenaActivator.getImageDescriptor("icons/aws_athena_logo.png"); //$NON-NLS-1$
    private final DriverPropertiesDialogPage driverPropsPage;
    private Button showCatalogsCheck;

    public AthenaConnectionPage() {
        driverPropsPage = new DriverPropertiesDialogPage(this);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite) {
        setImageDescriptor(logoImage);

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = e -> site.updateButtons();

        {
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, AthenaMessages.label_connection, 2, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            awsRegionCombo = UIUtils.createLabelCombo(addrGroup, AthenaMessages.label_region, SWT.DROP_DOWN);
            awsRegionCombo.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, AthenaMessages.label_s3_location); //$NON-NLS-2$ //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-1$
            Composite s3Group = UIUtils.createComposite(addrGroup, 1);
            s3Group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            s3LocationText = new Text(s3Group, SWT.BORDER);
            s3LocationText.setToolTipText(AthenaMessages.label_s3_output_location);
            s3LocationText.addModifyListener(textListener);
            s3LocationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            if (FileSystemProviderRegistry.getInstance().getProvider("aws-s3") != null) {
                ((GridLayout) s3Group.getLayout()).numColumns++;
                UIUtils.createPushButton(s3Group, UIConnectionMessages.controls_client_home_selector_browse, DBeaverIcons.getImage(DBIcon.TREE_FOLDER), new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(getSite().getProject());
                        DBNFileSystems fsRootNode = projectNode.getExtraNode(DBNFileSystems.class);
                        if (fsRootNode == null) {
                            DBWorkbench.getPlatformUI().showMessageBox("Cloud support required", "Project file system node not found", true);
                            return;
                        }
                        DBNNode selectedNode = null;
                        String oldS3Path = s3LocationText.getText();
                        if (!CommonUtils.isEmpty(oldS3Path) && oldS3Path.startsWith("s3:/")) {
                            selectedNode = findFileSystemNode(fsRootNode, oldS3Path);
                        }
                        ObjectBrowserDialogBase dialog = new ObjectBrowserDialogBase(
                            s3LocationText.getShell(), "S3 browser",
                            fsRootNode,
                            CommonUtils.singletonOrEmpty(selectedNode),
                            true
                        ) {
                            @Override
                            protected boolean matchesResultNode(DBNNode node) {
                                return node instanceof DBNPathBase &&
                                    Files.isDirectory(((DBNPathBase) node).getPath());
                            }

                            @Override
                            protected ViewerFilter createViewerFilter() {
                                return new ViewerFilter() {
                                    @Override
                                    public boolean select(Viewer viewer, Object parentElement, Object element) {
                                        return
                                            element instanceof TreeNodeSpecial ||
                                                element instanceof DBNFileSystem ||
                                                (element instanceof DBNNode && matchesResultNode((DBNNode) element));
                                    }
                                };
                            }
                        };
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            List<DBNNode> selectedObjects = dialog.getSelectedObjects();
                            if (selectedObjects.size() == 1) {
                                DBNNode s3Node = selectedObjects.get(0);
                                if (s3Node instanceof DBNPathBase) {
                                    String newS3Path = ((DBNPathBase) s3Node).getPath().toString();
                                    if (newS3Path.startsWith("s3:/")) {
                                        try {
                                            URI uri = new URI(newS3Path);
                                            URI patchedURI = new URI(uri.getScheme(), null, null, 0, uri.getPath(), uri.getQuery(), null);
                                            s3LocationText.setText(patchedURI.toString());
                                        } catch (URISyntaxException ex) {
                                            DBWorkbench.getPlatformUI().showError("Bad URI", "Bad URI '" + newS3Path + "'", ex);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
            }

            UIUtils.addVariablesToControl(s3LocationText, getAvailableVariables(), "S3 location pattern");

            showCatalogsCheck = UIUtils.createCheckbox(addrGroup,
                "Show catalogs",
                "To show multiple data catalogs with Athena (for example, when using an external Hive metastore or federated queries)", false, 2);
        }

        createAuthPanel(settingsGroup, 1);


        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    private DBNNode findFileSystemNode(DBNFileSystems fsRootNode, String s3Path) {
        final DBNPathBase[] result = new DBNPathBase[1];
        RuntimeUtils.runTask(monitor -> {
            try {
                result[0] = fsRootNode.findNodeByPath(monitor, s3Path, true);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }, "Load S3 node", 10000);
        return result[0];
    }

    @Override
    public boolean isComplete() {
        return awsRegionCombo != null && !CommonUtils.isEmpty(awsRegionCombo.getText()) &&
            s3LocationText != null && !CommonUtils.isEmpty(s3LocationText.getText()) &&
            super.isComplete();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();

        if (awsRegionCombo != null) {
            awsRegionCombo.removeAll();
            for (AWSRegion region : AWSRegion.values()) {
                awsRegionCombo.add(region.getId());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
                awsRegionCombo.setText(connectionInfo.getServerName());
            }
            if (awsRegionCombo.getText().isEmpty()) {
                awsRegionCombo.setText(AWSRegion.us_west_1.getId());
            }
        }

        if (s3LocationText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName)) {
                databaseName = connectionInfo.getProviderProperty(AthenaConstants.DRIVER_PROP_S3_OUTPUT_LOCATION);
                if (CommonUtils.isEmpty(databaseName)) {
                    databaseName = "s3://aws-athena-query-results-"; //$NON-NLS-1$
                }
            }
            s3LocationText.setText(databaseName);
        }
        if (showCatalogsCheck != null) {
            showCatalogsCheck.setSelection(
                CommonUtils.getBoolean(connectionInfo.getProviderProperty(AthenaConstants.PROP_SHOW_CATALOGS))
            );
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (awsRegionCombo != null) {
            connectionInfo.setServerName(awsRegionCombo.getText().trim());
        }
        if (s3LocationText != null) {
            connectionInfo.setProviderProperty(AthenaConstants.DRIVER_PROP_S3_OUTPUT_LOCATION, s3LocationText.getText().trim());
            connectionInfo.setDatabaseName(s3LocationText.getText().trim());
        }
        if (showCatalogsCheck != null) {
            connectionInfo.setProviderProperty(AthenaConstants.PROP_SHOW_CATALOGS, String.valueOf(showCatalogsCheck.getSelection()));
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[]{
            driverPropsPage
        };
    }

    @NotNull
    private String[] getAvailableVariables() {
        return Arrays.stream(DBPConnectionConfiguration.INTERNAL_CONNECT_VARIABLES).map(x -> x[0]).toArray(String[]::new);
    }

}
