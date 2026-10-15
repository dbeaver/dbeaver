/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.controls.ExpandableCompositeEx;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostgreCreateSubscriptionPage extends BaseObjectEditPage {
    private static final int DEFAULT_PAGE_HEIGHT_IN_LINES = 32;
    private final PostgreSubscription subscription;
    private Text nameText;
    private PostgreSubscriptionConnectionPanel publisherPanel;
    private Text connectionText;
    private Text publicationsText;
    private Button customConnection;
    private Button connectButton;
    private Button enabledButton;
    private UIObservable<Boolean> createSlot;
    private UIObservable<Boolean> copyData;
    private Button binaryButton;
    private Button streamingButton;
    private Text slotText;
    private Combo commitCombo;
    private Button testConnectionButton;
    private Button loadPublicationsButton;
    private Label publicationsStatus;
    private Label connectionTestStatus;
    private TabFolder tabs;
    private Composite publicationsList;
    private StackLayout publicationsListLayout;
    private Composite publicationsEmptyArea;
    private Label publicationsEmpty;
    private Label publicationsSource;
    private ComboViewer publicationDatasources;
    private Text publicationsFilter;
    private CheckboxTableViewer publicationsViewer;
    private List<String> availablePublications = List.of();
    private AbstractJob publicationsJob;
    private long publisherRevision;
    private long publicationsRevision;
    private boolean resizePending;

    public PostgreCreateSubscriptionPage(@NotNull PostgreSubscription subscription) {
        super(PostgreMessages.dialog_create_subscription_title);
        this.subscription = subscription;
    }

    @NotNull
    @Override
    public PostgreSubscription getObject() {
        return subscription;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        tabs = new TabFolder(parent, SWT.NONE);
        GridData pageData = new GridData(GridData.FILL_BOTH);
        pageData.widthHint = UIUtils.getFontHeight(tabs) * 45;
        pageData.heightHint = UIUtils.getFontHeight(tabs) * DEFAULT_PAGE_HEIGHT_IN_LINES;
        tabs.setLayoutData(pageData);

        Composite generalPage = createTab(tabs, PostgreMessages.dialog_create_subscription_general_tab);
        Composite general = UIUtils.createComposite(generalPage, 2);
        general.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameText = UIUtils.createLabelText(general, PostgreMessages.dialog_create_subscription_name, subscription.getName());
        nameText.setToolTipText(PostgreMessages.dialog_create_subscription_name_tip);
        nameText.selectAll();
        Text subscriber = UIUtils.createLabelText(general, PostgreMessages.dialog_create_subscription_subscriber,
            subscription.getDataSource().getContainer().getName() + " / " + subscription.getDatabase().getName(), SWT.BORDER | SWT.READ_ONLY);
        subscriber.setToolTipText(PostgreMessages.dialog_create_subscription_subscriber_tip);

        Composite connection = createTab(tabs, PostgreMessages.dialog_create_subscription_connection_tab);
        createPublisherControls(connection);
        createConnectionTestControls(connection);
        Composite publications = createTab(tabs, PostgreMessages.dialog_create_subscription_publications_tab);
        createPublicationControls(publications);
        Composite settings = createTab(tabs, PostgreMessages.dialog_create_subscription_settings_tab);
        createStartupControls(settings);
        createAdvancedControls(settings);

        for (Text text : new Text[]{nameText, connectionText, publicationsText, slotText}) {
            text.addModifyListener(event -> {
                validateProperties();
                updateTestConnectionButton();
                if (text == connectionText) {
                    publisherChanged();
                } else if (text == publicationsText) {
                    refreshPublicationSelection();
                }
            });
        }
        parent.addDisposeListener(event -> cancelPublicationRequest());
        for (Composite page : new Composite[]{generalPage, connection, publications, settings}) {
            UIUtils.configureScrolledComposite((ScrolledComposite) page.getParent(), page);
        }
        tabs.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateSize()));
        return tabs;
    }

    @NotNull
    private static Composite createTab(@NotNull TabFolder tabs, @NotNull String title) {
        TabItem tab = new TabItem(tabs, SWT.NONE);
        tab.setText(title);
        ScrolledComposite scroller = UIUtils.createScrolledComposite(tabs, SWT.V_SCROLL);
        Composite page = UIUtils.createComposite(scroller, 1);
        tab.setControl(scroller);
        return page;
    }

    private void createPublisherControls(@NotNull Composite page) {
        customConnection = UIUtils.createCheckbox(page, PostgreMessages.dialog_create_subscription_custom_connection, false);
        customConnection.setToolTipText(PostgreMessages.dialog_create_subscription_connection_string_tip);
        publisherPanel = new PostgreSubscriptionConnectionPanel(page, subscription.getDatabase(),
            new DBPConnectionConfiguration(), () -> {
                if (publisherPanel != null) {
                    publisherChanged();
                    UIUtils.refreshScrolledComposite((ScrolledComposite) page.getParent());
                }
            }, this::updateSize);
        Composite custom = UIUtils.createComposite(page, 2);
        custom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        connectionText = UIUtils.createLabelText(custom, PostgreMessages.dialog_create_subscription_connection_string,
            subscription.getConnectionInfo(), SWT.BORDER | SWT.PASSWORD);
        connectionText.setToolTipText(PostgreMessages.dialog_create_subscription_connection_string_tip);
        UIUtils.setControlVisible(custom, false);
        customConnection.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            boolean useCustom = customConnection.getSelection();
            UIUtils.setControlVisible(publisherPanel, !useCustom);
            UIUtils.setControlVisible(custom, useCustom);
            publisherChanged();
            updateSize();
        }));
    }

    @NotNull
    private String getPublisherConnectionInfo() {
        return customConnection.getSelection() ? connectionText.getText() : publisherPanel.getConnectionInfo();
    }

    private void updateTestConnectionButton() {
        if (testConnectionButton != null) {
            testConnectionButton.setEnabled(!getPublisherConnectionInfo().isBlank());
        }
    }

    private void createConnectionTestControls(@NotNull Composite page) {
        Composite actions = UIUtils.createComposite(page, 2);
        actions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        connectionTestStatus = createHint(actions, "");
        ((GridData) connectionTestStatus.getLayoutData()).widthHint = 0;
        testConnectionButton = UIUtils.createDialogButton(actions, PostgreMessages.dialog_create_subscription_test_connection,
            SelectionListener.widgetSelectedAdapter(event -> testPublisherConnection()));
        testConnectionButton.setToolTipText(PostgreMessages.dialog_create_subscription_test_connection_tip);
        updateTestConnectionButton();
    }

    private void createPublicationControls(@NotNull Composite page) {
        Composite source = UIUtils.createComposite(page, 2);
        source.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(source, PostgreMessages.dialog_create_subscription_publications_datasource);
        // Keep datasource selection independent of shell-owned popup controls used by custom combo widgets.
        publicationDatasources = new ComboViewer(source, SWT.DROP_DOWN | SWT.READ_ONLY);
        publicationDatasources.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        publicationDatasources.setContentProvider(ArrayContentProvider.getInstance());
        publicationDatasources.setLabelProvider(new DatabaseLabelProviders.ConnectionLabelProvider() {
                @NotNull
                @Override
                public String getText(@Nullable Object element) {
                    return element instanceof DBPDataSourceContainer ? super.getText(element)
                        : PostgreMessages.dialog_create_subscription_select_datasource;
                }
            });
        List<Object> items = new ArrayList<>();
        items.add(PostgreMessages.dialog_create_subscription_select_datasource);
        for (DBPDataSourceContainer candidate : subscription.getDataSource().getContainer().getRegistry().getDataSources()) {
            if (!candidate.isHidden() && !candidate.isTemporary() && PostgreSubscription.supportsPublicationDiscoveryConnection(candidate)) {
                items.add(candidate);
            }
        }
        publicationDatasources.setInput(items);
        publicationDatasources.setSelection(new StructuredSelection(items.getFirst()));
        publicationDatasources.getCombo().setToolTipText(PostgreMessages.dialog_create_subscription_publications_datasource_tip);
        Composite publications = UIUtils.createComposite(page, 3);
        publications.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(publications, PostgreMessages.dialog_create_subscription_publications);
        publicationsText = new Text(publications, SWT.BORDER | SWT.SINGLE);
        publicationsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        publicationsText.setText(PostgreSubscription.formatPublicationNames(subscription.getPublications().lines().toList()));
        publicationsText.setToolTipText(PostgreMessages.dialog_create_subscription_publications_tip);
        loadPublicationsButton = UIUtils.createDialogButton(publications, PostgreMessages.dialog_create_subscription_load_publications,
            SelectionListener.widgetSelectedAdapter(event -> loadPublications()));
        publicationsStatus = createHint(page, "");
        UIUtils.setControlVisible(publicationsStatus, false);
        publicationsSource = createHint(page, "");
        publicationsFilter = new Text(page, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        publicationsFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        publicationsFilter.setMessage(PostgreMessages.dialog_create_subscription_filter_publications);
        publicationsList = new Composite(page, SWT.BORDER);
        publicationsListLayout = new StackLayout();
        publicationsList.setLayout(publicationsListLayout);
        GridData listData = new GridData(GridData.FILL_BOTH);
        listData.heightHint = UIUtils.getFontHeight(page) * 12;
        publicationsList.setLayoutData(listData);
        publicationsEmptyArea = UIUtils.createComposite(publicationsList, 1);
        publicationsEmpty = new Label(publicationsEmptyArea, SWT.WRAP | SWT.CENTER);
        GridData emptyData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        emptyData.widthHint = UIUtils.getFontHeight(page) * 28;
        publicationsEmpty.setLayoutData(emptyData);
        publicationsEmpty.setText(PostgreMessages.dialog_create_subscription_publications_empty);
        publicationsViewer = CheckboxTableViewer.newCheckList(publicationsList, SWT.V_SCROLL);
        publicationsList.setBackground(publicationsViewer.getTable().getBackground());
        publicationsList.setBackgroundMode(SWT.INHERIT_FORCE);
        publicationsViewer.setContentProvider(ArrayContentProvider.getInstance());
        publicationsViewer.setLabelProvider(new LabelProvider());
        publicationsViewer.setInput(availablePublications);
        publicationsViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(@NotNull Viewer viewer, @Nullable Object parentElement, @NotNull Object element) {
                return ((String) element).toLowerCase(Locale.ROOT).contains(publicationsFilter.getText().toLowerCase(Locale.ROOT));
            }
        });
        publicationsFilter.addModifyListener(event -> {
            publicationsViewer.refresh();
            refreshPublicationList();
        });
        publicationsViewer.addCheckStateListener(event -> {
            String name = (String) event.getElement();
            try {
                // Change only the toggled name, preserving checked names hidden by the search filter and manual entries.
                String current = String.join("\n", PostgreSubscription.parsePublicationNames(publicationsText.getText()));
                String selected = PostgreSubscription.mergePublicationSelection(current, List.of(name),
                    event.getChecked() ? List.of(name) : List.of());
                publicationsText.setText(PostgreSubscription.formatPublicationNames(selected.lines().toList()));
            } catch (DBException e) {
                publicationsViewer.setChecked(name, !event.getChecked());
                setPublicationStatus(PostgreMessages.dialog_create_subscription_error_publication_list);
            }
        });
        refreshPublicationList();
        updatePublicationSource();
        updateTestConnectionButton();
        updateLoadPublicationsButton();
        publicationDatasources.addSelectionChangedListener(event -> {
            cancelPublicationRequest();
            availablePublications = List.of();
            publicationsViewer.setInput(availablePublications);
            publicationsFilter.setText("");
            refreshPublicationList();
            setPublicationStatus("");
            updatePublicationSource();
            updateLoadPublicationsButton();
            if (getSelectedPublicationDatasource() != null) {
                loadPublications();
            }
        });
    }

    private void testPublisherConnection() {
        String connectionInfo = getPublisherConnectionInfo();
        long revision = publisherRevision;
        connectionTestStatus.setText(PostgreMessages.dialog_create_subscription_testing_connection);
        updateSize();
        try {
            Boolean succeeded = UIUtils.runWithDialog(monitor -> {
                PostgreSubscription.testPublisherConnection(monitor, subscription.getDatabase(), connectionInfo);
                return !monitor.isCanceled();
            });
            if (!connectionTestStatus.isDisposed() && revision == publisherRevision) {
                connectionTestStatus.setText(Boolean.TRUE.equals(succeeded)
                    ? PostgreMessages.dialog_create_subscription_connection_verified : "");
                connectionTestStatus.setToolTipText(PostgreMessages.dialog_create_subscription_test_connection_success);
            }
        } catch (DBException e) {
            if (!connectionTestStatus.isDisposed() && revision == publisherRevision) {
                connectionTestStatus.setText(e.getMessage());
                connectionTestStatus.setToolTipText(e.getMessage());
            }
        }
        if (!connectionTestStatus.isDisposed()) {
            updateSize();
        }
    }

    private void cancelPublicationRequest() {
        publicationsRevision++;
        if (publicationsJob != null) {
            publicationsJob.cancel();
            publicationsJob = null;
        }
    }

    private void publisherChanged() {
        publisherRevision++;
        if (connectionTestStatus != null) {
            connectionTestStatus.setText("");
            connectionTestStatus.setToolTipText(null);
        }
        if (slotText != null) {
            validateProperties();
        }
        updateTestConnectionButton();
    }

    private void updateLoadPublicationsButton() {
        if (loadPublicationsButton != null) {
            loadPublicationsButton.setEnabled(publicationsJob == null && getSelectedPublicationDatasource() != null);
        }
    }

    private void updatePublicationSource() {
        DBPDataSourceContainer selected = getSelectedPublicationDatasource();
        String source = selected != null
            ? NLS.bind(PostgreMessages.dialog_create_subscription_publications_source,
                selected.getName())
            : PostgreMessages.dialog_create_subscription_select_datasource;
        loadPublicationsButton.setToolTipText(source + "\n" + PostgreMessages.dialog_create_subscription_load_publications_tip);
        publicationsSource.setText(source);
    }

    private void setPublicationStatus(@NotNull String status) {
        publicationsStatus.setText(status);
        refreshPublicationList();
        updateSize();
    }

    private void loadPublications() {
        DBPDataSourceContainer selectedConnection = getSelectedPublicationDatasource();
        if (selectedConnection == null) {
            return;
        }
        long revision = publicationsRevision;
        setPublicationStatus(PostgreMessages.dialog_create_subscription_loading_publications);
        publicationsJob = new AbstractJob(PostgreMessages.dialog_create_subscription_load_publications) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    // Saved desktop settings (including tunnels and SSL) stay independent of the subscriber-side form.
                    List<String> names = PostgreSubscription.readPublisherPublicationNames(
                        monitor, selectedConnection);
                    UIUtils.asyncExec(() -> {
                        if (!publicationsText.isDisposed() && revision == publicationsRevision) {
                            availablePublications = names;
                            publicationsViewer.setInput(names);
                            refreshPublicationList();
                            setPublicationStatus(names.isEmpty()
                                ? PostgreMessages.dialog_create_subscription_no_publications : "");
                        }
                    });
                } catch (Exception e) {
                    // Driver error messages may contain connection credentials. Show only a safe, actionable message.
                    UIUtils.asyncExec(() -> {
                        if (!publicationsText.isDisposed() && revision == publicationsRevision) {
                            setPublicationStatus(PostgreMessages.dialog_create_subscription_load_publications_error);
                        }
                    });
                } finally {
                    UIUtils.asyncExec(() -> {
                        if (!publicationsText.isDisposed() && revision == publicationsRevision) {
                            publicationsJob = null;
                            updateLoadPublicationsButton();
                        }
                    });
                }
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        };
        updateLoadPublicationsButton();
        publicationsJob.schedule();
    }

    private void refreshPublicationSelection() {
        try {
            publicationsViewer.setCheckedElements(PostgreSubscription.parsePublicationNames(publicationsText.getText()).toArray());
        } catch (DBException e) {
            publicationsViewer.setCheckedElements(new Object[0]);
        }
    }

    @Nullable
    private DBPDataSourceContainer getSelectedPublicationDatasource() {
        Object selected = publicationDatasources.getStructuredSelection().getFirstElement();
        return selected instanceof DBPDataSourceContainer container ? container : null;
    }

    private void refreshPublicationList() {
        boolean hasPublications = !availablePublications.isEmpty();
        boolean hasMatches = hasPublications && publicationsViewer.getTable().getItemCount() > 0;
        publicationsListLayout.topControl = hasMatches ? publicationsViewer.getTable() : publicationsEmptyArea;
        UIUtils.setControlVisible(publicationsFilter, hasPublications);
        String status = publicationsStatus.getText();
        // Empty, loading, and failed requests share the same centered state instead of duplicating messages above the list.
        publicationsEmpty.setText(hasPublications ? PostgreMessages.dialog_create_subscription_publications_no_matches
            : status.isBlank() ? PostgreMessages.dialog_create_subscription_publications_empty : status);
        UIUtils.setControlVisible(publicationsStatus, hasPublications && !status.isBlank());
        publicationsList.layout(true, true);
        refreshPublicationSelection();
        updateSize();
    }

    public void updateSize() {
        if (resizePending || tabs == null || tabs.isDisposed()) {
            return;
        }
        resizePending = true;
        UIUtils.asyncExec(() -> {
            resizePending = false;
            if (tabs.isDisposed() || !getShell().isVisible() || tabs.getSelectionIndex() < 0) {
                return;
            }
            ScrolledComposite scroller = (ScrolledComposite) tabs.getSelection()[0].getControl();
            Control content = scroller.getContent();
            if (content == null) {
                return;
            }
            // Measure the active page without its scroll viewport's height limit, so expanded SSL can grow the shell.
            GridData data = (GridData) tabs.getLayoutData();
            int width = scroller.getClientArea().width;
            int preferredHeight = content.computeSize(width > 0 ? width : data.widthHint, SWT.DEFAULT, true).y;
            data.heightHint = Math.max(UIUtils.getFontHeight(tabs) * DEFAULT_PAGE_HEIGHT_IN_LINES,
                preferredHeight + tabs.computeTrim(0, 0, 0, 0).height);
            Shell shell = getShell();
            Rectangle current = shell.getBounds();
            Point preferred = shell.computeSize(current.width, SWT.DEFAULT, true);
            Rectangle screen = shell.getMonitor().getClientArea();
            int height = Math.min(screen.height, Math.max(current.height, preferred.y));
            int y = Math.max(screen.y, Math.min(current.y, screen.y + screen.height - height));
            shell.setBounds(current.x, y, current.width, height);
            shell.layout(true, true);
            UIUtils.refreshScrolledComposite(scroller);
        });
    }

    private void createStartupControls(@NotNull Composite page) {
        Composite startup = UIUtils.createTitledComposite(page, PostgreMessages.dialog_create_subscription_startup, 1, GridData.FILL_HORIZONTAL);
        connectButton = UIUtils.createCheckbox(startup, PostgreMessages.dialog_create_subscription_connect, subscription.isConnect());
        connectButton.setToolTipText(PostgreMessages.dialog_create_subscription_connect_tip);
        enabledButton = UIUtils.createCheckbox(startup, PostgreMessages.dialog_create_subscription_enabled, subscription.isEnabled());
        enabledButton.setToolTipText(PostgreMessages.dialog_create_subscription_enabled_tip);
        UIObservable<Boolean> connect = UIObservable.of(connectButton.getSelection());
        createSlot = UIObservable.of(subscription.isCreateSlot());
        copyData = UIObservable.of(subscription.isCopyData());
        Control options = UIPanelBuilder.build(startup, panel -> panel
            .row(row -> row.enabled(connect).checkBox(PostgreMessages.dialog_create_subscription_create_slot,
                PostgreMessages.dialog_create_subscription_create_slot_tip, button -> button.enabled(connect).selected(createSlot)))
            .row(row -> row.enabled(connect).checkBox(PostgreMessages.dialog_create_subscription_copy_data,
                PostgreMessages.dialog_create_subscription_copy_data_tip, button -> button.enabled(connect).selected(copyData))));
        options.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        connectButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            boolean selected = connectButton.getSelection();
            connect.set(selected);
            enabledButton.setEnabled(selected);
            if (!selected) {
                enabledButton.setSelection(false);
                copyData.set(false);
                createSlot.set(false);
            }
            validateProperties();
        }));
    }

    private void createAdvancedControls(@NotNull Composite page) {
        ExpandableCompositeEx section = createExpandableSection(page, PostgreMessages.dialog_create_subscription_advanced);
        Composite advanced = UIUtils.createComposite(section, 2);
        section.setClient(advanced);

        slotText = UIUtils.createLabelText(advanced, PostgreMessages.dialog_create_subscription_slot, "");
        slotText.setToolTipText(PostgreMessages.dialog_create_subscription_slot_tip);
        binaryButton = UIUtils.createCheckbox(advanced, PostgreMessages.dialog_create_subscription_binary, subscription.isBinary());
        binaryButton.setToolTipText(PostgreMessages.dialog_create_subscription_binary_tip);
        streamingButton = UIUtils.createCheckbox(advanced, PostgreMessages.dialog_create_subscription_streaming, subscription.isStreaming());
        streamingButton.setToolTipText(PostgreMessages.dialog_create_subscription_streaming_tip);
        commitCombo = UIUtils.createLabelCombo(advanced, PostgreMessages.dialog_create_subscription_commit, SWT.READ_ONLY | SWT.DROP_DOWN);
        commitCombo.setToolTipText(PostgreMessages.dialog_create_subscription_commit_tip);
        commitCombo.setItems(PostgreSubscription.getSynchronousCommitModes());
        commitCombo.setText(subscription.getSynchronousCommit());
        UIUtils.createInfoLabel(advanced, PostgreMessages.dialog_create_subscription_disconnected_tip, GridData.FILL_HORIZONTAL, 2);

    }

    @NotNull
    private ExpandableCompositeEx createExpandableSection(@NotNull Composite parent, @NotNull String title) {
        ExpandableCompositeEx section = UIUtils.createExpandableCompositeWithSeparator(parent, SWT.NONE,
            ExpandableCompositeEx.TWISTIE | ExpandableCompositeEx.CLIENT_INDENT);
        section.setText(title);
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        section.addExpansionListener(IExpansionListener.expansionStateChangedAdapter(event -> updateSize()));
        return section;
    }

    @NotNull
    private static Label createHint(@NotNull Composite parent, @NotNull String text) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = UIUtils.getFontHeight(parent) * 35;
        label.setLayoutData(data);
        return label;
    }

    @Nullable
    @Override
    protected String getEditError() {
        if (nameText == null) {
            return null;
        }
        if (nameText.getText().isBlank()) {
            return PostgreMessages.dialog_create_subscription_error_name;
        }
        if (customConnection.getSelection()) {
            if (connectionText.getText().isBlank()) {
                return PostgreMessages.dialog_create_subscription_error_connection;
            }
        } else {
            if (publisherPanel.getConnectionInfo().isBlank()) {
                return PostgreMessages.dialog_create_subscription_error_publisher;
            }
        }
        if (publicationsText.getText().isBlank()) {
            return PostgreMessages.dialog_create_subscription_error_publications;
        }
        try {
            PostgreSubscription.parsePublicationNames(publicationsText.getText());
        } catch (DBException e) {
            return PostgreMessages.dialog_create_subscription_error_publication_list;
        }
        return null;
    }

    @Override
    public boolean isPageComplete() {
        return getEditError() == null;
    }

    @Override
    public void performFinish() throws DBException {
        String error = getEditError();
        if (error != null) {
            throw new DBException(error);
        }
        subscription.setName(nameText.getText().trim());
        subscription.setConnectionInfo(getPublisherConnectionInfo());
        subscription.setPublications(String.join("\n", PostgreSubscription.parsePublicationNames(publicationsText.getText())));
        subscription.setConnect(connectButton.getSelection());
        subscription.setEnabled(enabledButton.getSelection());
        subscription.setCreateSlot(createSlot.get());
        subscription.setCopyData(copyData.get());
        subscription.setSlotName(slotText.getText().trim());
        subscription.setBinary(binaryButton.getSelection());
        subscription.setStreaming(streamingButton.getSelection());
        subscription.setSynchronousCommit(commitCombo.getText());
        subscription.getCreateStatement();
    }
}
