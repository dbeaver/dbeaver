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
package org.jkiss.dbeaver.ui.gis.panel;

import com.vividsolutions.jts.geom.Geometry;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.Map;

public class GISLeafletViewer {

    private static final Log log = Log.getLog(GISLeafletViewer.class);

    private final IValueController valueController;
    private final Browser browser;
    private DBGeometry[] lastValue;
    private int sourceSRID; // Explicitly set SRID
    private int actualSourceSRID; // SRID taken from geometry value
    private File scriptFile;
    private final ToolBarManager toolBarManager;
    private int defaultSRID; // Target SRID used to render map

    private boolean toolsVisible = true;
    private final Composite composite;

    public GISLeafletViewer(Composite parent, IValueController valueController) {
        this.valueController = valueController;

        composite = UIUtils.createPlaceholder(parent, 1);
        CSSUtils.setCSSClass(composite, DBStyles.COLORED_BY_CONNECTION_TYPE);

        browser = new Browser(composite, SWT.NONE);
        browser.addDisposeListener(e -> {
            cleanupFiles();
        });
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite bottomPanel = UIUtils.createPlaceholder(composite, 1);//new Composite(composite, SWT.NONE);
            bottomPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            CSSUtils.setCSSClass(bottomPanel, DBStyles.COLORED_BY_CONNECTION_TYPE);

            ToolBar bottomToolbar = new ToolBar(bottomPanel, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);

            toolBarManager = new ToolBarManager(bottomToolbar);
        }
    }

    private void setSourceSRID(int srid) {
        if (srid == sourceSRID) {
            return;
        }
        int oldSRID = sourceSRID;
        this.sourceSRID = srid;
        try {
            reloadGeometryData(lastValue, true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Setting SRID", "Can't change source SRID to " + srid, e);
            sourceSRID = oldSRID;
        }
    }

    public void setGeometryData(@Nullable DBGeometry[] values) throws DBException {
        reloadGeometryData(values, false);
    }

    public void reloadGeometryData(@Nullable DBGeometry[] values, boolean force) throws DBException {
        if (!force && CommonUtils.equalObjects(lastValue, values)) {
            return;
        }
        if (browser != null) {
            try {
                if (ArrayUtils.isEmpty(values)) {
                    browser.setUrl("about:blank");
                } else {
                    File file = generateViewScript(values);
                    browser.setUrl(file.toURI().toURL().toString());
                }
            } catch (IOException e) {
                throw new DBException("Error generating viewer script", e);
            }
        }
        lastValue = values;
        updateToolbar();
    }

    private File generateViewScript(DBGeometry[] values) throws IOException {
        if (scriptFile == null) {
            File tempDir = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "gis-viewer-files");
            checkIncludesExistence(tempDir);

            scriptFile = File.createTempFile("view", "gis.html", tempDir);
        }
        int baseSRID = 0;
        String[] geomValues = new String[values.length];
        String[] geomTipValues = new String[values.length];
        boolean showMap = false;
        for (int i = 0; i < values.length; i++) {
            DBGeometry value = values[i];
            Object targetValue = value.getRawValue();
            int srid = sourceSRID == 0 ? value.getSRID() : sourceSRID;
            if (srid == GisConstants.DEFAULT_SRID) {
                showMap = true;
            } else {
                Geometry geometry = value.getGeometry();
                if (geometry != null) {
                    try {
                        GisTransformRequest request = new GisTransformRequest(geometry, srid, GisConstants.DEFAULT_SRID);
                        GisTransformUtils.transformGisData(request);
                        targetValue = request.getTargetValue();
                        srid = request.getTargetSRID();
                        actualSourceSRID = request.getSourceSRID();
                        showMap = request.isShowOnMap();
                    } catch (DBException e) {
                        log.debug("Error transforming CRS", e);
                        showMap = false;
                    }
                }
            }
            if (srid == 0) {
                srid = GisConstants.DEFAULT_SRID;
                showMap = true; // Let's give it a try
            }
            if (actualSourceSRID == 0) {
                actualSourceSRID = GisConstants.DEFAULT_SRID;
            }
            if (baseSRID == 0) {
                baseSRID = srid;
            }

            if (targetValue == null) {
                continue;
            }
            geomValues[i] = "'" + targetValue + "'";
            if (CommonUtils.isEmpty(value.getProperties())) {
                geomTipValues[i] = "null";
            } else {
                StringBuilder geomProps = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Object> prop : value.getProperties().entrySet()) {
                    if (!first) geomProps.append(",");
                    first = false;
                    geomProps.append('"').append(prop.getKey().replace("\"", "\\\"")).append("\":\"")
                        .append(DBValueFormatting.getDefaultValueDisplayString(prop.getValue(), DBDDisplayFormat.UI).replace("\"", "\\\"")).append("\"");
                }
                geomProps.append("}");
                geomTipValues[i] = geomProps.toString();
            }
        }
        if (baseSRID == 0) {
            if (valueController != null && valueController.getValueType() instanceof GisAttribute) {
                try {
                    baseSRID = ((GisAttribute) valueController.getValueType()).getAttributeGeometrySRID(new VoidProgressMonitor());
                } catch (DBCException e) {
                    log.error(e);
                }
            }
        }
        if (baseSRID == 0) {
            baseSRID = GisConstants.DEFAULT_SRID;
        }
        this.defaultSRID = baseSRID;
        String geomValuesString = String.join(",", geomValues);
        String geomTipValuesString = String.join(",", geomTipValues);
        boolean isShowMap = showMap;

        InputStream fis = GISViewerActivator.getDefault().getResourceStream(GISBrowserViewerConstants.VIEW_TEMPLATE_PATH);
        if (fis == null) {
            throw new IOException("View template file not found (" + GISBrowserViewerConstants.VIEW_TEMPLATE_PATH + ")");
        }
        try (InputStreamReader isr = new InputStreamReader(fis)) {
            String viewTemplate = IOUtils.readToString(isr);
            viewTemplate = GeneralUtils.replaceVariables(viewTemplate, name -> {
                if (name.equals("geomValues")) {
                    return geomValuesString;
                } else if (name.equals("geomTipValues")) {
                    return String.valueOf(geomTipValuesString);
                } else if (name.equals("geomSRID")) {
                    return String.valueOf(defaultSRID);
                } else if (name.equals("showMap")) {
                    return String.valueOf(isShowMap);
                } else if (name.equals("showTools")) {
                    return String.valueOf(toolsVisible);
                }
                return null;
            });
            try (FileOutputStream fos = new FileOutputStream(scriptFile)) {
                fos.write(viewTemplate.getBytes(GeneralUtils.UTF8_CHARSET));
            }
        } finally {
            ContentUtils.close(fis);
        }

        return scriptFile;
    }

    private void checkIncludesExistence(File scriptDir) throws IOException {
        File incFolder = new File(scriptDir, "inc");
        if (!incFolder.exists()) {
            if (!incFolder.mkdirs()) {
                throw new IOException("Can't create inc folder '" + incFolder.getAbsolutePath() + "'");
            }
            for (String fileName : GISBrowserViewerConstants.INC_FILES) {
                InputStream fis = GISViewerActivator.getDefault().getResourceStream(GISBrowserViewerConstants.WEB_INC_PATH + fileName);
                if (fis != null) {
                    try (FileOutputStream fos = new FileOutputStream(new File(incFolder, fileName))) {
                        try {
                            IOUtils.copyStream(fis, fos);
                        } catch (Exception e) {
                            log.warn("Error copying inc file " + fileName, e);
                        } finally {
                            ContentUtils.close(fis);
                        }
                    }
                }
            }
        }
    }

    private void cleanupFiles() {
        if (scriptFile != null) {
            if (!scriptFile.delete()) {
                log.debug("Can't delete temp script file '" + scriptFile.getAbsolutePath() + "'");
            }
        }
    }

    public Composite getBrowserComposite() {
        return composite;
    }

    public Browser getBrowser() {
        return browser;
    }

    public DBGeometry[] getCurrentValue() {
        return lastValue;
    }

    private int getCurrentSourceSRID() {
        return actualSourceSRID != 0 ? actualSourceSRID :
            defaultSRID != 0 ? defaultSRID : GisConstants.DEFAULT_SRID;
    }

    private void updateToolbar() {
        toolBarManager.removeAll();
        toolBarManager.add(new Action("Open in browser", DBeaverIcons.getImageDescriptor(UIIcon.BROWSER)) {
            @Override
            public void run() {
                UIUtils.launchProgram(scriptFile.getAbsolutePath());
            }
        });
        toolBarManager.add(new Action("Copy as picture", DBeaverIcons.getImageDescriptor(UIIcon.PICTURE)) {
            @Override
            public void run() {
                Image image = new Image(Display.getDefault(), browser.getBounds());
                GC gc = new GC(image);
                try {
                    browser.print(gc);
                } finally {
                    gc.dispose();
                }
                ImageTransfer imageTransfer = ImageTransfer.getInstance();
                Clipboard clipboard = new Clipboard(Display.getCurrent());
                clipboard.setContents(new Object[] {image.getImageData()}, new Transfer[]{imageTransfer});
            }
        });
        toolBarManager.add(new Action("Save as picture", DBeaverIcons.getImageDescriptor(UIIcon.PICTURE_SAVE)) {
            @Override
            public void run() {
/*
                Image image = new Image(Display.getDefault(), browser.getBounds());
                GC gc = new GC(image);
                try {
                    browser.print(gc);
                } finally {
                    gc.dispose();
                }
                ImageTransfer imageTransfer = ImageTransfer.getInstance();
                Clipboard clipboard = new Clipboard(Display.getCurrent());
                clipboard.setContents(new Object[] {image.getImageData()}, new Transfer[]{imageTransfer});
*/
            }
        });

        toolBarManager.add(new Action("Print", DBeaverIcons.getImageDescriptor(UIIcon.PRINT)) {
            @Override
            public void run() {
                GC gc = new GC(browser.getDisplay());
                try {
                    browser.execute("javascript:window.print();");
                } finally {
                    gc.dispose();
                }
            }
        });

        Action crsSelectorAction = new ChangeCRSAction();
        toolBarManager.add(ActionUtils.makeActionContribution(crsSelectorAction, true));

        toolBarManager.add(new Separator());

        toolBarManager.add(new Action("Show/Hide controls", Action.AS_CHECK_BOX) {
            {
                setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.PALETTE));
            }

            @Override
            public boolean isChecked() {
                return toolsVisible;
            }

            @Override
            public void run() {
                toolsVisible = !toolsVisible;
                updateControlsVisibility();
                updateToolbar();
            }
        });

        toolBarManager.update(true);
    }

    private void updateControlsVisibility() {
        GC gc = new GC(browser.getDisplay());
        try {
            browser.execute("javascript:showTools(" + toolsVisible +");");
        } finally {
            gc.dispose();
        }
    }

    private class ChangeCRSAction extends Action implements IMenuCreator {

        private MenuManager menuManager;

        public ChangeCRSAction() {
            super("EPSG:" + getCurrentSourceSRID(), Action.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CHART_LINE));
        }

        @Override
        public void run() {
            SelectCRSDialog selectCRSDialog = new SelectCRSDialog(
                UIUtils.getActiveWorkbenchShell(),
                getCurrentSourceSRID());
            if (selectCRSDialog.open() == IDialogConstants.OK_ID) {
                setSourceSRID(selectCRSDialog.getSelectedSRID());
            }
        }

        @Override
        public IMenuCreator getMenuCreator() {
            return super.getMenuCreator();
        }

        @Override
        public void dispose() {
            if (menuManager != null) {
                menuManager.dispose();
                menuManager = null;
            }
        }

        @Override
        public Menu getMenu(Control parent) {
            if (menuManager == null) {
                menuManager = new MenuManager();
                menuManager.add(new SetCRSAction(GisConstants.DEFAULT_SRID));
                menuManager.add(new Action("Other ...") {
                    @Override
                    public void run() {
                        ChangeCRSAction.this.run();
                    }
                });
            }
            return menuManager.createContextMenu(parent);
        }

        @Override
        public Menu getMenu(Menu parent) {
            return null;
        }
    }

    private class SetCRSAction extends Action {
        private final int srid;

        public SetCRSAction(int srid) {
            super("EPSG:" + srid);
            this.srid = srid;
        }

        @Override
        public void run() {
            setSourceSRID(srid);
        }
    }

}