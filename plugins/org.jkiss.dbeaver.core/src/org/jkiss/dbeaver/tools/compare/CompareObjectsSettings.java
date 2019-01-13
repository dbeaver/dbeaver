/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.util.List;

/**
 * Compare settings
 */
public class CompareObjectsSettings {

    public enum OutputType {
        BROWSER("Open in browser"),
        FILE("Save to file");
        private final String title;

        private OutputType(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private final List<DBNDatabaseNode> nodes;
    private boolean skipSystemObjects = true;
    private boolean compareLazyProperties = false;
    private boolean compareOnlyStructure = false;
    private boolean compareScripts = false;
    private boolean showOnlyDifferences = false;
    private OutputType outputType = OutputType.BROWSER;
    private String outputFolder = DialogUtils.getCurDialogFolder();

    public CompareObjectsSettings(List<DBNDatabaseNode> nodes) {
        this.nodes = nodes;
    }

    public List<DBNDatabaseNode> getNodes() {
        return nodes;
    }

    public boolean isSkipSystemObjects() {
        return skipSystemObjects;
    }

    public void setSkipSystemObjects(boolean skipSystemObjects) {
        this.skipSystemObjects = skipSystemObjects;
    }

    public boolean isCompareLazyProperties() {
        return compareLazyProperties;
    }

    public void setCompareLazyProperties(boolean compareLazyProperties) {
        this.compareLazyProperties = compareLazyProperties;
    }

    public boolean isCompareOnlyStructure() {
        return compareOnlyStructure;
    }

    public void setCompareOnlyStructure(boolean compareOnlyStructure) {
        this.compareOnlyStructure = compareOnlyStructure;
    }

    public boolean isCompareScripts() {
        return compareScripts;
    }

    public void setCompareScripts(boolean compareScripts) {
        this.compareScripts = compareScripts;
    }

    public boolean isShowOnlyDifferences() {
        return showOnlyDifferences;
    }

    public void setShowOnlyDifferences(boolean showOnlyDifferences) {
        this.showOnlyDifferences = showOnlyDifferences;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    void loadFrom(IDialogSettings dialogSettings) {
        if (dialogSettings.get("skipSystem") != null) {
            skipSystemObjects = dialogSettings.getBoolean("skipSystem");
        }
        if (dialogSettings.get("compareLazy") != null) {
            compareLazyProperties = dialogSettings.getBoolean("compareLazy");
        }
        if (dialogSettings.get("compareStructure") != null) {
            compareOnlyStructure = dialogSettings.getBoolean("compareStructure");
        }
        if (dialogSettings.get("showDifference") != null) {
            showOnlyDifferences = dialogSettings.getBoolean("showDifference");
        }
        if (dialogSettings.get("compareScripts") != null) {
            compareScripts = dialogSettings.getBoolean("compareScripts");
        }
        if (dialogSettings.get("outputType") != null) {
            outputType = OutputType.valueOf(dialogSettings.get("outputType"));
        }
        if (dialogSettings.get("outputFolder") != null) {
            outputFolder = dialogSettings.get("outputFolder");
        }
    }

    void saveTo(IDialogSettings dialogSettings) {
        dialogSettings.put("skipSystem", skipSystemObjects);
        dialogSettings.put("compareLazy", compareLazyProperties);
        dialogSettings.put("compareStructure", compareOnlyStructure);
        dialogSettings.put("compareScripts", compareScripts);
        dialogSettings.put("showDifference", showOnlyDifferences);
        dialogSettings.put("outputType", outputType.name());
        dialogSettings.put("outputFolder", outputFolder);
    }

}
