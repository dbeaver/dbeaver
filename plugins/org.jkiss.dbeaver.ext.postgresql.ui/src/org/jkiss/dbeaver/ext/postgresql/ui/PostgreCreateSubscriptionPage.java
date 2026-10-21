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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.controls.ExpandableCompositeEx;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PostgreCreateSubscriptionPage extends BaseObjectEditPage {
    private static final int DEFAULT_PAGE_HEIGHT_IN_LINES = 20;
    private static final int ADVANCED_LABEL_WIDTH = 150;
    private static final int COMMIT_CONTROL_WIDTH = 200;
    private static final int FIELD_LABEL_TOP_MARGIN = 3;

    private enum Step {
        PUBLISHER, PUBLICATIONS, REPLICATION
    }

    private Step currentStep = Step.PUBLISHER;
    private Composite pages;
    private StackLayout pageLayout;
    private final List<ScrolledComposite> scrollers = new ArrayList<>();
    private final Set<String> selectedPublications = new LinkedHashSet<>();
    private final Set<String> customPublications = new LinkedHashSet<>();
    private final PostgreSubscription subscription;
    private Text nameText;
    private PostgreSubscriptionConnectionPanel publisherPanel;
    private Text customPublicationText;
    private Button connectButton;
    private Button enabledButton;
    private Button createSlot;
    private Button copyData;
    private Button binaryButton;
    private Button streamingButton;
    private Text slotText;
    private Combo commitCombo;
    private Button testConnectionButton;
    private Button loadPublicationsButton;
    private Label publicationsStatus;
    private Label connectionTestStatus;
    private Composite publicationsList;
    private StackLayout publicationsListLayout;
    private Composite publicationsEmptyArea;
    private Label publicationsEmpty;
    private Button publicationSource;
    private Button publisherSetup;
    private Boolean publisherLogicalReplicationEnabled;
    private boolean publicationsDirty = true;
    private Text publicationsFilter;
    private CheckboxTableViewer publicationsViewer;
    private List<String> availablePublications = List.of();
    private AbstractJob publicationsJob;
    private long publisherRevision;
    private long publicationsRevision;
    private String publicationSourceKey;
    private boolean resizePending;

    public PostgreCreateSubscriptionPage(@NotNull PostgreSubscription subscription) {
        super(PostgreMessages.dialog_create_subscription_title);
        this.subscription = subscription;
        subscription.getPublications().lines().filter(name -> !name.isBlank()).forEach(selectedPublications::add);
        customPublications.addAll(selectedPublications);
    }

    @NotNull
    @Override
    public PostgreSubscription getObject() {
        return subscription;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        pages = new Composite(parent, SWT.NONE);
        pageLayout = new StackLayout();
        pages.setLayout(pageLayout);
        GridData pageData = new GridData(GridData.FILL_BOTH);
        pageData.widthHint = UIUtils.getFontHeight(pages) * 45;
        pageData.heightHint = UIUtils.getFontHeight(pages) * DEFAULT_PAGE_HEIGHT_IN_LINES;
        pages.setLayoutData(pageData);

        Composite connection = createStep();
        Composite general = UIUtils.createComposite(connection, 2);
        general.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridLayout) general.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        nameText = UIUtils.createLabelText(general, PostgreMessages.dialog_create_subscription_name, subscription.getName());
        nameText.setToolTipText(PostgreMessages.dialog_create_subscription_name_tip);
        nameText.selectAll();
        UIUtils.createControlLabel(general, PostgreMessages.dialog_create_subscription_subscriber);
        Composite subscriber = UIUtils.createComposite(general, 2);
        subscriber.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label subscriberIcon = new Label(subscriber, SWT.NONE);
        subscriberIcon.setImage(new DatabaseLabelProviders.ConnectionLabelProvider().getImage(subscription.getDataSource().getContainer()));
        Label subscriberName = UIUtils.createLabel(subscriber,
            subscription.getDataSource().getContainer().getName() + " / " + subscription.getDatabase().getName());
        subscriberName.setToolTipText(PostgreMessages.dialog_create_subscription_subscriber_tip);

        createPublisherControls(connection);
        publisherPanel.alignLabels(general);
        createConnectionTestControls(publisherPanel.getTestArea());
        Composite publications = createStep();
        createPublicationControls(publications);
        Composite settings = createStep();
        createStartupControls(settings);
        createAdvancedControls(settings);

        for (Text text : new Text[]{nameText, slotText}) {
            text.addModifyListener(event -> {
                validateProperties();
            });
        }
        parent.addDisposeListener(event -> cancelPublicationRequest());
        for (ScrolledComposite scroller : scrollers) {
            UIUtils.configureScrolledComposite(scroller, (Composite) scroller.getContent());
        }
        pageLayout.topControl = scrollers.getFirst();
        return pages;
    }

    @NotNull
    private Composite createStep() {
        ScrolledComposite scroller = UIUtils.createScrolledComposite(pages, SWT.V_SCROLL);
        Composite page = UIUtils.createComposite(scroller, 1);
        ((GridLayout) page.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.SECTION_SPACING;
        scroller.setContent(page);
        scrollers.add(scroller);
        return page;
    }

    @NotNull
    public String getStepTitle() {
        return switch (currentStep) {
            case PUBLISHER -> PostgreMessages.dialog_create_subscription_publisher;
            case PUBLICATIONS -> PostgreMessages.dialog_create_subscription_publications_tab;
            case REPLICATION -> PostgreMessages.dialog_create_subscription_settings_tab;
        };
    }

    @NotNull
    public String getStepDescription() {
        return switch (currentStep) {
            case PUBLISHER -> PostgreMessages.dialog_create_subscription_publisher_description;
            case PUBLICATIONS -> PostgreMessages.dialog_create_subscription_publications_description;
            case REPLICATION -> PostgreMessages.dialog_create_subscription_settings_description;
        };
    }

    public boolean hasPreviousStep() {
        return currentStep != Step.PUBLISHER;
    }

    public boolean hasNextStep() {
        return currentStep != Step.REPLICATION;
    }

    public boolean canAdvance() {
        return getEditError() == null;
    }

    public void changeStep(boolean forward) {
        if (forward ? !hasNextStep() || !canAdvance() : !hasPreviousStep()) {
            return;
        }
        currentStep = Step.values()[currentStep.ordinal() + (forward ? 1 : -1)];
        pageLayout.topControl = scrollers.get(currentStep.ordinal());
        pages.layout(true, true);
        validateProperties();
        updateSize();
        if (currentStep == Step.PUBLICATIONS && publicationsDirty) {
            loadPublications();
        }
    }

    private void createPublisherControls(@NotNull Composite page) {
        publisherPanel = new PostgreSubscriptionConnectionPanel(page, subscription.getDatabase(),
            new DBPConnectionConfiguration(), () -> {
                if (publisherPanel != null) {
                    publisherChanged();
                    UIUtils.refreshScrolledComposite(scrollers.getFirst());
                }
            }, this::updateSize);
    }

    @NotNull
    private String getPublisherConnectionInfo() {
        return publisherPanel.getConnectionInfo();
    }

    private void updateTestConnectionButton() {
        if (testConnectionButton != null) {
            testConnectionButton.setEnabled(!getPublisherConnectionInfo().isBlank());
        }
    }

    private void createConnectionTestControls(@NotNull Composite page) {
        Composite actions = page;
        testConnectionButton = UIUtils.createDialogButton(actions, PostgreMessages.dialog_create_subscription_test_connection,
            SelectionListener.widgetSelectedAdapter(event -> testPublisherConnection()));
        testConnectionButton.setToolTipText(PostgreMessages.dialog_create_subscription_test_connection_tip);
        connectionTestStatus = createHint(actions, PostgreMessages.dialog_create_subscription_connection_not_tested);
        ((GridData) connectionTestStatus.getLayoutData()).widthHint = 0;
        updateTestConnectionButton();
    }

    private void createPublicationControls(@NotNull Composite page) {
        ((GridLayout) page.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        Composite source = UIUtils.createComposite(page, 3);
        ((GridLayout) source.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.COMPACT_SPACING;
        source.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(source, PostgreMessages.dialog_create_subscription_publications_datasource);
        publicationSource = UIUtils.createDialogButton(source, "",
            SelectionListener.widgetSelectedAdapter(event -> publisherPanel.showPublicationSourcesMenu(publicationSource)));
        publicationSource.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        publicationSource.setToolTipText(PostgreMessages.dialog_create_subscription_publications_datasource_tip);
        loadPublicationsButton = UIUtils.createDialogButton(source, PostgreMessages.dialog_create_subscription_load_publications,
            SelectionListener.widgetSelectedAdapter(event -> loadPublications()));
        publicationsStatus = createHint(page, "");
        UIUtils.setControlVisible(publicationsStatus, false);
        Composite publications = UIUtils.createComposite(page, 1);
        publications.setLayoutData(new GridData(GridData.FILL_BOTH));
        ((GridLayout) publications.getLayout()).marginWidth = 0;
        ((GridLayout) publications.getLayout()).marginHeight = 0;
        publicationsFilter = new Text(publications, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        publicationsFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        publicationsFilter.setMessage(PostgreMessages.dialog_create_subscription_filter_publications);
        publicationsList = new Composite(publications, SWT.BORDER);
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
        refreshPublicationInput();
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
            if (event.getChecked()) {
                selectedPublications.add(name);
            } else {
                selectedPublications.remove(name);
            }
            validateProperties();
        });
        Composite manual = UIUtils.createComposite(publications, 2);
        manual.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        customPublicationText = new Text(manual, SWT.BORDER | SWT.SINGLE);
        customPublicationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        customPublicationText.setMessage(PostgreMessages.dialog_create_subscription_custom_publication_name);
        Button addPublication = UIUtils.createDialogButton(manual, PostgreMessages.dialog_create_subscription_add_publication,
            SelectionListener.widgetSelectedAdapter(event -> addCustomPublication()));
        addPublication.setEnabled(false);
        customPublicationText.addModifyListener(event -> addPublication.setEnabled(!customPublicationText.getText().isBlank()));
        customPublicationText.addTraverseListener(event -> {
            if (event.detail == SWT.TRAVERSE_RETURN) {
                addCustomPublication();
                event.doit = false;
            }
        });
        refreshPublicationList();
        updatePublicationSource();
        updateTestConnectionButton();
        updateLoadPublicationsButton();
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
            getShell().forceActive();
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
        publisherLogicalReplicationEnabled = null;
        updatePublisherSetup();
        if (connectionTestStatus != null) {
            connectionTestStatus.setText(PostgreMessages.dialog_create_subscription_connection_not_tested);
            connectionTestStatus.setToolTipText(null);
        }
        updateTestConnectionButton();
        publicationsDirty = true;
        if (publicationSource != null) {
            cancelPublicationRequest();
            availablePublications = List.of();
            String sourceKey = publisherPanel.getPublicationSourceKey();
            if (sourceKey != null) {
                if (publicationSourceKey != null && !publicationSourceKey.equals(sourceKey)) {
                    selectedPublications.clear();
                    customPublications.clear();
                }
                publicationSourceKey = sourceKey;
            }
            refreshPublicationInput();
            publicationsFilter.setText("");
            setPublicationStatus("");
            updatePublicationSource();
            updateLoadPublicationsButton();
        }
        if (slotText != null) {
            validateProperties();
        }
    }

    private void updateLoadPublicationsButton() {
        if (loadPublicationsButton != null) {
            loadPublicationsButton.setEnabled(publicationsJob == null && !getPublisherConnectionInfo().isBlank());
        }
    }

    private void updatePublicationSource() {
        String source = publisherPanel.getPublicationSourceName();
        publicationSource.setText(source + (publisherPanel.hasCopiedConnection() ? " \u25BE" : ""));
        publicationSource.setEnabled(publisherPanel.hasCopiedConnection());
        loadPublicationsButton.setToolTipText(NLS.bind(PostgreMessages.dialog_create_subscription_publications_source, source)
            + "\n" + PostgreMessages.dialog_create_subscription_load_publications_tip);
    }

    private void setPublicationStatus(@NotNull String status) {
        publicationsStatus.setText(status);
        refreshPublicationList();
        updateSize();
    }

    private void loadPublications() {
        if (publicationsJob != null || getPublisherConnectionInfo().isBlank()) {
            return;
        }
        DataSourceDescriptor selectedConnection;
        try {
            selectedConnection = publisherPanel.createPublicationConnection();
        } catch (DBException e) {
            setPublicationStatus(PostgreMessages.dialog_create_subscription_load_publications_error);
            return;
        }
        publicationsDirty = false;
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
                    // use the publisher selected on the first step, with its desktop access settings
                    List<String> names = PostgreSubscription.readPublisherPublicationNames(
                        monitor, selectedConnection);
                    boolean logicalReplicationEnabled = PostgreSubscription.isPublisherLogicalReplicationEnabled(monitor, selectedConnection);
                    UIUtils.asyncExec(() -> {
                        if (!publicationsViewer.getTable().isDisposed() && revision == publicationsRevision) {
                            publisherLogicalReplicationEnabled = logicalReplicationEnabled;
                            availablePublications = names;
                            refreshPublicationInput();
                            refreshPublicationList();
                            setPublicationStatus(names.isEmpty() ? PostgreMessages.dialog_create_subscription_no_publications : "");
                            updatePublisherSetup();
                            validateProperties();
                        }
                    });
                } catch (Exception e) {
                    // Driver error messages may contain connection credentials. Show only a safe, actionable message.
                    UIUtils.asyncExec(() -> {
                        if (!publicationsViewer.getTable().isDisposed() && revision == publicationsRevision) {
                            setPublicationStatus(PostgreMessages.dialog_create_subscription_load_publications_error);
                        }
                    });
                } finally {
                    selectedConnection.disconnect(new VoidProgressMonitor());
                    selectedConnection.dispose();
                    UIUtils.asyncExec(() -> {
                        if (!publicationsViewer.getTable().isDisposed() && revision == publicationsRevision) {
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

    private void addCustomPublication() {
        String name = customPublicationText.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        customPublications.add(name);
        selectedPublications.add(name);
        customPublicationText.setText("");
        publicationsFilter.setText("");
        refreshPublicationInput();
        refreshPublicationList();
        validateProperties();
    }

    private void refreshPublicationInput() {
        Set<String> names = new LinkedHashSet<>(availablePublications);
        names.addAll(customPublications);
        names.addAll(selectedPublications);
        publicationsViewer.setInput(names);
    }

    private void refreshPublicationSelection() {
        publicationsViewer.setCheckedElements(selectedPublications.toArray());
    }

    private void refreshPublicationList() {
        boolean hasPublications = !availablePublications.isEmpty() || !customPublications.isEmpty() || !selectedPublications.isEmpty();
        boolean hasMatches = hasPublications && publicationsViewer.getTable().getItemCount() > 0;
        publicationsListLayout.topControl = hasMatches ? publicationsViewer.getTable() : publicationsEmptyArea;
        String status = publicationsStatus.getText();
        // keep loading and failure messages visible when there are no publications
        publicationsEmpty.setText(hasPublications ? PostgreMessages.dialog_create_subscription_publications_no_matches
            : status.isBlank() ? PostgreMessages.dialog_create_subscription_publications_empty : status);
        UIUtils.setControlVisible(publicationsStatus, hasPublications && !status.isBlank());
        publicationsList.layout(true, true);
        refreshPublicationSelection();
        updateSize();
    }

    public void updateSize() {
        if (resizePending || pages == null || pages.isDisposed()) {
            return;
        }
        resizePending = true;
        UIUtils.asyncExec(() -> {
            resizePending = false;
            if (pages.isDisposed() || !getShell().isVisible()) {
                return;
            }
            ScrolledComposite scroller = scrollers.get(currentStep.ordinal());
            Control content = scroller.getContent();
            if (content == null) {
                return;
            }
            // measure only the active step, including expanded sections
            GridData data = (GridData) pages.getLayoutData();
            int width = scroller.getClientArea().width;
            int preferredHeight = content.computeSize(width > 0 ? width : data.widthHint, SWT.DEFAULT, true).y;
            int minimumHeight = currentStep == Step.REPLICATION ? 0 : UIUtils.getFontHeight(pages) * DEFAULT_PAGE_HEIGHT_IN_LINES;
            data.heightHint = Math.max(minimumHeight, preferredHeight);
            Shell shell = getShell();
            Rectangle current = shell.getBounds();
            Point preferred = shell.computeSize(current.width, SWT.DEFAULT, true);
            Rectangle screen = shell.getMonitor().getClientArea();
            int height = Math.min(screen.height, preferred.y);
            int y = Math.max(screen.y, Math.min(current.y, screen.y + screen.height - height));
            shell.setBounds(current.x, y, current.width, height);
            shell.layout(true, true);
            UIUtils.refreshScrolledComposite(scroller);
        });
    }

    private void createStartupControls(@NotNull Composite page) {
        Composite startup = UIUtils.createComposite(page, 1);
        startup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridLayout) startup.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        connectButton = UIUtils.createCheckbox(startup, PostgreMessages.dialog_create_subscription_connect, subscription.isConnect());
        connectButton.setToolTipText(PostgreMessages.dialog_create_subscription_connect_tip);
        Composite dependent = UIUtils.createComposite(startup, 1);
        GridData dependentData = new GridData(GridData.FILL_HORIZONTAL);
        dependentData.horizontalIndent = PostgreSubscriptionDialogLayout.MARGIN;
        dependent.setLayoutData(dependentData);
        ((GridLayout) dependent.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        enabledButton = UIUtils.createCheckbox(dependent, PostgreMessages.dialog_create_subscription_enabled, subscription.isEnabled());
        enabledButton.setToolTipText(PostgreMessages.dialog_create_subscription_enabled_tip);
        createSlot = UIUtils.createCheckbox(dependent,
            PostgreMessages.dialog_create_subscription_create_slot, subscription.isCreateSlot());
        createSlot.setToolTipText(PostgreMessages.dialog_create_subscription_create_slot_tip);
        copyData = UIUtils.createCheckbox(dependent, PostgreMessages.dialog_create_subscription_copy_data, subscription.isCopyData());
        copyData.setToolTipText(PostgreMessages.dialog_create_subscription_copy_data_tip);
        createSlot.setEnabled(connectButton.getSelection());
        copyData.setEnabled(connectButton.getSelection());
        enabledButton.setEnabled(connectButton.getSelection());
        connectButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            boolean selected = connectButton.getSelection();
            createSlot.setEnabled(selected);
            copyData.setEnabled(selected);
            enabledButton.setEnabled(selected);
            if (!selected) {
                enabledButton.setSelection(false);
                copyData.setSelection(false);
                createSlot.setSelection(false);
            }
            updatePublisherSetup();
            validateProperties();
        }));
        createSlot.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            updatePublisherSetup();
            validateProperties();
        }));
        publisherSetup = UIUtils.createDialogButton(startup, PostgreMessages.dialog_create_subscription_publisher_setup,
            SelectionListener.widgetSelectedAdapter(event -> MessageDialog.openInformation(getShell(),
                PostgreMessages.dialog_create_subscription_publisher_setup,
                PostgreMessages.dialog_create_subscription_publisher_setup_instructions)));
        publisherSetup.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        updatePublisherSetup();
    }

    private void updatePublisherSetup() {
        if (publisherSetup != null) {
            UIUtils.setControlVisible(publisherSetup, Boolean.FALSE.equals(publisherLogicalReplicationEnabled)
                && connectButton.getSelection() && createSlot.getSelection());
            updateSize();
        }
    }

    private void createAdvancedControls(@NotNull Composite page) {
        ExpandableCompositeEx section = createExpandableSection(page, PostgreMessages.dialog_create_subscription_advanced);
        Composite advanced = UIUtils.createComposite(section, 1);
        GridLayout layout = (GridLayout) advanced.getLayout();
        layout.marginLeft = PostgreSubscriptionDialogLayout.MARGIN;
        layout.verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        section.setClient(advanced);

        Composite slot = UIUtils.createComposite(advanced, 2);
        slot.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label slotLabel = UIUtils.createControlLabel(slot, PostgreMessages.dialog_create_subscription_slot);
        Composite slotValue = UIUtils.createComposite(slot, 1);
        slotValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        slotText = new Text(slotValue, SWT.BORDER | SWT.SINGLE);
        slotText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        slotText.setToolTipText(PostgreMessages.dialog_create_subscription_slot_tip);
        slotText.setMessage(PostgreMessages.dialog_create_subscription_slot_placeholder);

        binaryButton = UIUtils.createCheckbox(advanced, PostgreMessages.dialog_create_subscription_binary, subscription.isBinary());
        binaryButton.setToolTipText(PostgreMessages.dialog_create_subscription_binary_tip);
        streamingButton = UIUtils.createCheckbox(advanced,
            PostgreMessages.dialog_create_subscription_streaming, subscription.isStreaming());
        streamingButton.setToolTipText(PostgreMessages.dialog_create_subscription_streaming_tip);
        Composite commit = UIUtils.createComposite(advanced, 2);
        commit.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        commitCombo = UIUtils.createLabelCombo(commit, PostgreMessages.dialog_create_subscription_commit, SWT.READ_ONLY | SWT.DROP_DOWN);
        commitCombo.setToolTipText(PostgreMessages.dialog_create_subscription_commit_tip);
        GridData commitData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        commitData.widthHint = COMMIT_CONTROL_WIDTH;
        commitCombo.setLayoutData(commitData);
        commitCombo.setItems(PostgreSubscription.getSynchronousCommitModes());
        commitCombo.setText(subscription.getSynchronousCommit());
        PostgreSubscriptionDialogLayout.alignLabels(ADVANCED_LABEL_WIDTH, slot, commit);
        GridData slotLabelData = (GridData) slotLabel.getLayoutData();
        slotLabelData.verticalAlignment = SWT.TOP;
        slotLabelData.verticalIndent = FIELD_LABEL_TOP_MARGIN;
    }

    @NotNull
    private ExpandableCompositeEx createExpandableSection(@NotNull Composite parent, @NotNull String title) {
        ExpandableCompositeEx section = UIUtils.createExpandableCompositeWithSeparator(parent, SWT.NONE,
            ExpandableCompositeEx.TWISTIE);
        section.setShowTextAsTitle(true);
        section.clientVerticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
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
        return switch (currentStep) {
            case PUBLISHER -> getPublisherError();
            case PUBLICATIONS -> getPublicationsError();
            case REPLICATION -> getReplicationError();
        };
    }

    @Nullable
    private String getPublisherError() {
        if (nameText.getText().isBlank()) {
            return PostgreMessages.dialog_create_subscription_error_name;
        }
        if (getPublisherConnectionInfo().isBlank()) {
            return PostgreMessages.dialog_create_subscription_error_publisher;
        }
        return null;
    }

    @Nullable
    private String getPublicationsError() {
        return selectedPublications.isEmpty() ? PostgreMessages.dialog_create_subscription_error_publications : null;
    }

    @Nullable
    private String getReplicationError() {
        String error = getPublisherError() != null ? getPublisherError() : getPublicationsError();
        if (error != null) {
            return error;
        }
        String slot = slotText.getText().isBlank() ? nameText.getText().trim() : slotText.getText().trim();
        if (!PostgreSubscription.isValidSlotName(slot)) {
            return PostgreMessages.dialog_create_subscription_error_slot;
        }
        if (Boolean.FALSE.equals(publisherLogicalReplicationEnabled) && connectButton.getSelection() && createSlot.getSelection()) {
            return PostgreMessages.dialog_create_subscription_publisher_wal_level;
        }
        return null;
    }

    @Override
    public boolean isPageComplete() {
        return currentStep == Step.REPLICATION && getEditError() == null;
    }

    @Override
    public void performFinish() throws DBException {
        String error = getReplicationError();
        if (error != null) {
            throw new DBException(error);
        }
        subscription.setName(nameText.getText().trim());
        subscription.setConnectionInfo(getPublisherConnectionInfo());
        subscription.setPublications(String.join("\n", selectedPublications));
        subscription.setConnect(connectButton.getSelection());
        subscription.setEnabled(enabledButton.getSelection());
        subscription.setCreateSlot(createSlot.getSelection());
        subscription.setCopyData(copyData.getSelection());
        subscription.setSlotName(slotText.getText().trim());
        subscription.setBinary(binaryButton.getSelection());
        subscription.setStreaming(streamingButton.getSelection());
        subscription.setSynchronousCommit(commitCombo.getText());
        subscription.getCreateStatement();
    }
}
