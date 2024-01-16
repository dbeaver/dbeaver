/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditMavenArtifactDialog extends BaseDialog {
    private static final Log log = Log.getLog(EditMavenArtifactDialog.class);

    private static final Pattern REGEX_FOR_GRADLE = Pattern.compile("([\\w_.-]+):([\\w_.-]+)(?::([\\w_.-]+))(?::([\\w_.-]+))?", Pattern.MULTILINE);

    private final DriverLibraryMavenArtifact originalArtifact;
    private final DriverDescriptor driver;
    private final List<DriverLibraryMavenArtifact> artifacts = new ArrayList<>();

    private boolean ignoreDependencies;
    private boolean loadOptionalDependencies;

    private Text groupText;
    private Text artifactText;
    private Text classifierText;
    private Text preferredVersionText;
    private Combo fallbackVersionText;
    private Text fieldText;

    private CLabel errorLabel;
    private TabFolder tabFolder;
    private boolean isReadOnly = false;

    public EditMavenArtifactDialog(@NotNull Shell shell, @NotNull DriverDescriptor driver, @Nullable DriverLibraryMavenArtifact library) {
        super(shell, UIConnectionMessages.dialog_edit_driver_edit_maven_title, DBIcon.TREE_USER);
        this.driver = driver;
        this.originalArtifact = library;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @NotNull
    public List<DriverLibraryMavenArtifact> getArtifacts() {
        return artifacts;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        {
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = UIUtils.getFontHeight(composite.getFont()) * 40;

            tabFolder = new TabFolder(composite, SWT.TOP | SWT.FLAT);
            tabFolder.setLayoutData(gd);
            tabFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    artifacts.clear();
                    if (tabFolder.getSelection()[0].getData() == TabType.DEPENDENCY_DECLARATION){
                        UIUtils.asyncExec(EditMavenArtifactDialog.this::parseArtifactText);
                    }
                }
            });
            if (originalArtifact == null) {
                createDependencyDeclarationTab(tabFolder);
            }
            createDeclareArtifactManuallyTab(tabFolder);
        }
        {
            Group settingsGroup = UIUtils.createControlGroup(composite, UIConnectionMessages.dialog_edit_driver_edit_maven_settings, 1, GridData.FILL_HORIZONTAL, 0);

            Button ignoreDependenciesCheckbox = UIUtils.createCheckbox(settingsGroup,
                UIConnectionMessages.dialog_edit_driver_edit_maven_ignore_transient_dependencies,
                UIConnectionMessages.dialog_edit_driver_edit_maven_load_optional_dependencies_tip,
                originalArtifact != null && originalArtifact.isIgnoreDependencies(),
                2);
            ignoreDependenciesCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ignoreDependencies = ignoreDependenciesCheckbox.getSelection();
                }
            });

            Button loadOptionalDependenciesCheckbox = UIUtils.createCheckbox(settingsGroup,
                UIConnectionMessages.dialog_edit_driver_edit_maven_load_optional_dependencies,
                UIConnectionMessages.dialog_edit_driver_edit_maven_load_optional_dependencies_tip,
                originalArtifact != null && originalArtifact.isLoadOptionalDependencies(),
                2);
            loadOptionalDependenciesCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    loadOptionalDependencies = loadOptionalDependenciesCheckbox.getSelection();
                }
            });
        }

        return composite;
    }

    private void parseArtifactText() {
        try {
            artifacts.clear();
            if (!fieldText.getText().isEmpty()) {
                artifacts.addAll(parseMaven());
                setStatus(false, NLS.bind(UIConnectionMessages.dialog_edit_driver_edit_maven_artifacts_count, artifacts.size()));
            }
            else {
                setStatus(false, UIConnectionMessages.dialog_edit_driven_edit_maven_field_text_message);
            }
        } catch (Exception e) {
            if (REGEX_FOR_GRADLE.matcher(fieldText.getText()).find()) {
                try {
                    artifacts.addAll(parseGradle());
                    setStatus(false, NLS.bind(UIConnectionMessages.dialog_edit_driver_edit_maven_artifacts_count, artifacts.size()));
                } catch (DBException ex) {
                    setStatus(true, e.getMessage());
                    log.debug("Error parsing dependency declaration", e);
                }
            } else {
                setStatus(true, e.getMessage());
                log.debug("Error parsing dependency declaration", e);
            }
        }
    }

    private void setStatus(boolean error, String message) {
        getButton(IDialogConstants.OK_ID).setEnabled(!error);
        errorLabel.setVisible(!message.isEmpty());
        if (!message.isEmpty()) {
            errorLabel.setImage(DBeaverIcons.getImage(error ? DBIcon.SMALL_ERROR : DBIcon.SMALL_INFO));
            errorLabel.setText(message);
        }
    }

    private void createDependencyDeclarationTab(@NotNull TabFolder folder) {

        Composite container = new Composite(folder, SWT.NONE);
        container.setLayout(new GridLayout(1, true));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(container.getFont()) * 12;

        fieldText = new Text(container, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);

        fieldText.setLayoutData(gd);
        fieldText.addModifyListener(event -> parseArtifactText());

        errorLabel = new CLabel(container, SWT.NONE);
        errorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        errorLabel.setVisible(false);

        //UIUtils.asyncExec(() -> setStatus(false, UIConnectionMessages.dialog_edit_driven_edit_maven_field_text_message));

        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText(UIConnectionMessages.dialog_edit_driver_edit_maven_raw);
        item.setControl(container);
        item.setData(TabType.DEPENDENCY_DECLARATION);
    }

    private void createDeclareArtifactManuallyTab(@NotNull TabFolder folder) {
        Composite container = new Composite(folder, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        container.setLayoutData(gd);

        groupText = UIUtils.createLabelText(container,
            UIConnectionMessages.dialog_edit_driver_edit_maven_group_id_label,
            originalArtifact != null ? CommonUtils.notEmpty(originalArtifact.getReference().getGroupId()) : "");
        artifactText = UIUtils.createLabelText(container,
            UIConnectionMessages.dialog_edit_driver_edit_maven_artifact_id_label,
            originalArtifact != null ? CommonUtils.notEmpty(originalArtifact.getReference().getArtifactId()) : "");
        classifierText = UIUtils.createLabelText(container,
            UIConnectionMessages.dialog_edit_driver_edit_maven_classifier_label,
            originalArtifact != null ? CommonUtils.notEmpty(originalArtifact.getReference().getClassifier()) : "");
        preferredVersionText = UIUtils.createLabelText(container,
            UIConnectionMessages.dialog_edit_driver_edit_maven_version_label,
            originalArtifact != null ? (originalArtifact.getPreferredVersion()) : "");
        fallbackVersionText = UIUtils.createLabelCombo(container,
            UIConnectionMessages.dialog_edit_driver_edit_maven_fallback_version_label,SWT.DROP_DOWN | SWT.BORDER);

        fallbackVersionText.add(MavenArtifactReference.VERSION_PATTERN_RELEASE);
        fallbackVersionText.add(MavenArtifactReference.VERSION_PATTERN_LATEST);
        if (originalArtifact != null) {
            fallbackVersionText.setText(CommonUtils.notEmpty(originalArtifact.getReference().getFallbackVersion()));
        }
        if (fallbackVersionText.getText().isEmpty()) {
            fallbackVersionText.select(0);
        }

        if (originalArtifact != null && !originalArtifact.isCustom()) {
            // Artifact reference is read-only. We use it to find libraries
            // To change artifact info delete it and create a new one
            groupText.setEditable(false);
            artifactText.setEditable(false);
            classifierText.setEditable(false);
            preferredVersionText.setEditable(false);
            fallbackVersionText.setEnabled(false);
            isReadOnly = true;
            UIUtils.createInfoLabel(container, "Predefined Maven artifacts are read-only", GridData.FILL_HORIZONTAL, 2);
        }

        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText(UIConnectionMessages.dialog_edit_driver_edit_maven_manual);
        item.setControl(container);
        item.setData(TabType.DECLARE_ARTIFACT_MANUALLY);

        ModifyListener ml = e -> updateButtons();
        groupText.addModifyListener(ml);
        artifactText.addModifyListener(ml);
        fallbackVersionText.addModifyListener(ml);
        preferredVersionText.addModifyListener(ml);
    }


    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(
            !CommonUtils.isEmpty(groupText.getText()) &&
                !CommonUtils.isEmpty(artifactText.getText()) &&
                !CommonUtils.isEmpty(fallbackVersionText.getText())
        );
    }

    private List<DriverLibraryMavenArtifact> parseGradle() throws DBException {
        final List<DriverLibraryMavenArtifact> artifacts = new ArrayList<>();
        final Matcher matcher = REGEX_FOR_GRADLE.matcher(fieldText.getText());
        while (matcher.find()) {
            String group = matcher.group(1);
            String name = matcher.group(2);
            String version = matcher.group(3);
            if (CommonUtils.isNotEmpty(group) && CommonUtils.isNotEmpty(name)) {
                DriverLibraryMavenArtifact lib = new DriverLibraryMavenArtifact(EditMavenArtifactDialog.this.driver,
                    DBPDriverLibrary.FileType.jar,
                    "",
                    version
                );
                lib.setReference(new MavenArtifactReference(
                    CommonUtils.notEmpty(group),
                    CommonUtils.notEmpty(name),
                    null,
                    null,
                    MavenArtifactReference.VERSION_PATTERN_RELEASE
                ));
                lib.setPreferredVersion(version);
                artifacts.add(lib);
            } else {
                throw new DBException("Wrong Gradle configuration: " + matcher.group());
            }
        }

        return artifacts;
    }

    @NotNull
    private List<DriverLibraryMavenArtifact> parseMaven() throws XMLException {
        final List<DriverLibraryMavenArtifact> artifacts = new ArrayList<>();

        try {
            SAXReader reader = new SAXReader(new StringReader(fieldText.getText()));
            reader.parse(new SAXMavenListener(artifacts));
        } catch (IOException e) {
            throw new XMLException("Error parsing XML", e);
        }

        return artifacts;
    }

    private class SAXMavenListener extends SAXListener.BaseListener {
        private final List<DriverLibraryMavenArtifact> artifacts;
        private final Deque<State> state;

        private String groupId;
        private String artifactId;
        private String classifier;
        private String version;

        public SAXMavenListener(@NotNull List<DriverLibraryMavenArtifact> artifacts) {
            this.artifacts = artifacts;
            this.state = new ArrayDeque<>();
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String name, Attributes atts) {
            if (state.isEmpty() && "dependencies".equals(name)) {
                state.offer(State.DEPENDENCIES);
            } else if ((state.isEmpty() || state.element() == State.DEPENDENCIES) && "dependency".equals(name)) {
                state.offer(State.DEPENDENCY);
                groupId = null;
                artifactId = null;
                classifier = null;
                version = null;
            } else if (state.peekLast() == State.DEPENDENCY && "groupId".equals(name)) {
                state.offer(State.DEPENDENCY_GROUP_ID);
            } else if (state.peekLast() == State.DEPENDENCY && "artifactId".equals(name)) {
                state.offer(State.DEPENDENCY_ARTIFACT_ID);
            } else if (state.peekLast() == State.DEPENDENCY && "classifier".equals(name)) {
                state.offer(State.DEPENDENCY_CLASSIFIER);
            } else if (state.peekLast() == State.DEPENDENCY && "version".equals(name)) {
                state.offer(State.DEPENDENCY_VERSION);
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String name) {
            if (state.peekLast() == State.DEPENDENCY && "dependency".equals(name)) {
                DriverLibraryMavenArtifact lib = new DriverLibraryMavenArtifact(EditMavenArtifactDialog.this.driver, DBPDriverLibrary.FileType.jar, "", version);
                lib.setReference(new MavenArtifactReference(groupId, artifactId, classifier, MavenArtifactReference.VERSION_PATTERN_RELEASE, version));
                lib.setPreferredVersion(version);
                artifacts.add(lib);
                state.removeLast();
            } else if ((state.peekLast() == State.DEPENDENCIES && "dependencies".equals(name))
                || (state.peekLast() == State.DEPENDENCY_GROUP_ID && "groupId".equals(name))
                || (state.peekLast() == State.DEPENDENCY_ARTIFACT_ID && "artifactId".equals(name))
                || (state.peekLast() == State.DEPENDENCY_CLASSIFIER && "classifier".equals(name))
                || (state.peekLast() == State.DEPENDENCY_VERSION && "version".equals(name))) {
                state.removeLast();
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) {
            if (state.isEmpty()) {
                return;
            }
            switch (state.peekLast()) {
                case DEPENDENCY_GROUP_ID:
                    groupId = data;
                    break;
                case DEPENDENCY_ARTIFACT_ID:
                    artifactId = data;
                    break;
                case DEPENDENCY_CLASSIFIER:
                    classifier = data;
                    break;
                case DEPENDENCY_VERSION:
                    version = data;
                    break;
                default:
                    break;
            }
        }

    }

    private enum TabType {
        DEPENDENCY_DECLARATION,
        DECLARE_ARTIFACT_MANUALLY
    }

    private enum State {
        DEPENDENCIES,
        DEPENDENCY,
        DEPENDENCY_GROUP_ID,
        DEPENDENCY_ARTIFACT_ID,
        DEPENDENCY_CLASSIFIER,
        DEPENDENCY_VERSION
    }

    @Override
    protected void okPressed() {
        if (isReadOnly) {
            super.okPressed();
            return;
        }
        if (tabFolder.getSelection()[0].getData() == TabType.DECLARE_ARTIFACT_MANUALLY) {
            if (originalArtifact != null) {
                originalArtifact.setReference(new MavenArtifactReference(
                    groupText.getText(),
                    artifactText.getText(),
                    CommonUtils.nullIfEmpty(classifierText.getText()),
                    CommonUtils.nullIfEmpty(fallbackVersionText.getText()),
                    preferredVersionText.getText()));
                originalArtifact.setPreferredVersion(preferredVersionText.getText().isEmpty() ? null : preferredVersionText.getText());
                originalArtifact.setIgnoreDependencies(ignoreDependencies);
                originalArtifact.setLoadOptionalDependencies(loadOptionalDependencies);
            } else {
                DriverLibraryMavenArtifact lib = new DriverLibraryMavenArtifact(
                    EditMavenArtifactDialog.this.driver,
                    DBPDriverLibrary.FileType.jar,
                    "",
                    preferredVersionText.getText().isEmpty() ? null : preferredVersionText.getText()
                );
                lib.setReference(new MavenArtifactReference(
                    groupText.getText(), artifactText.getText(), classifierText.getText(), fallbackVersionText.getText(), preferredVersionText.getText()));
                lib.setPreferredVersion(preferredVersionText.getText().isEmpty() ? null : preferredVersionText.getText());
                lib.setLoadOptionalDependencies(loadOptionalDependencies);
                lib.setIgnoreDependencies(ignoreDependencies);
                artifacts.add(lib);
            }
        } else {
            for (DriverLibraryMavenArtifact artifact : artifacts) {
                artifact.setLoadOptionalDependencies(loadOptionalDependencies);
                artifact.setIgnoreDependencies(ignoreDependencies);
            }
        }

        super.okPressed();
    }
}
