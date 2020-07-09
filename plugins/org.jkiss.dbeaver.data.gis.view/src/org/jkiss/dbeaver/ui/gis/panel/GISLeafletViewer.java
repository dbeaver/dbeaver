/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.gis.GeometryDataUtils;
import org.jkiss.dbeaver.ui.gis.GeometryViewerConstants;
import org.jkiss.dbeaver.ui.gis.IGeometryValueEditor;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.locationtech.jts.geom.Geometry;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GISLeafletViewer implements IGeometryValueEditor {

    private static final Log log = Log.getLog(GISLeafletViewer.class);

    private static final String PREF_RECENT_SRID_LIST = "srid.list.recent";

    private static final String[] SUPPORTED_FORMATS = new String[] { "png", "gif", "bmp" };

    private static final String PROP_FLIP_COORDINATES = "gis.flipCoords";
    private static final String PROP_SRID = "gis.srid";

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(DBDContent.class, new DBDContentAdapter()).create();

    private final IValueController valueController;
    private final Browser browser;
    private DBGeometry[] lastValue;
    private int sourceSRID; // Explicitly set SRID
    private int actualSourceSRID; // SRID taken from geometry value
    private File scriptFile;
    private final ToolBarManager toolBarManager;
    private int defaultSRID; // Target SRID used to render map

    private boolean toolsVisible = true;
    private boolean flipCoordinates = false;
    private final Composite composite;

    public GISLeafletViewer(Composite parent, IValueController valueController, SpatialDataProvider spatialDataProvider) {
        this.valueController = valueController;

        this.flipCoordinates = spatialDataProvider != null && spatialDataProvider.isFlipCoordinates();

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

        {
            String recentSRIDString = GISViewerActivator.getDefault().getPreferences().getString(PREF_RECENT_SRID_LIST);
            if (!CommonUtils.isEmpty(recentSRIDString)) {
                for (String sridStr : recentSRIDString.split(",")) {
                    int recentSRID = CommonUtils.toInt(sridStr);
                    if (recentSRID == 0 || recentSRID == GeometryDataUtils.getDefaultSRID() || recentSRID == GisConstants.SRID_3857) {
                        continue;
                    }
                    GISEditorUtils.addRecentSRID(recentSRID);
                }
            }
        }

        {
            // Check for save settings
            if (valueController instanceof IAttributeController) {
                DBDAttributeBinding binding = ((IAttributeController) valueController).getBinding();
                if (binding.getEntityAttribute() != null) {
                    DBVEntity vEntity = DBVUtils.getVirtualEntity(binding, false);
                    if (vEntity != null) {
                        DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, false);
                        if (vAttr != null) {
                            this.flipCoordinates = CommonUtils.getBoolean(vAttr.getProperty(PROP_FLIP_COORDINATES), this.flipCoordinates);
                            this.sourceSRID = CommonUtils.toInt(vAttr.getProperty(PROP_SRID), this.sourceSRID);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Control getEditorControl() {
        return composite;
    }

    @Override
    public int getValueSRID() {
        return actualSourceSRID;
    }

    @Override
    public void setValueSRID(int srid) {
        if (srid == sourceSRID) {
            //return;
        }
        int oldSRID = sourceSRID;
        this.sourceSRID = srid;
        try {
            reloadGeometryData(lastValue, true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Setting SRID", "Can't change source SRID to " + srid, e);
            sourceSRID = oldSRID;
        }
        {
            // Save SRID to the list of recently used SRIDs
            if (srid != GeometryDataUtils.getDefaultSRID() && srid != GisConstants.SRID_3857) {
                GISEditorUtils.addRecentSRID(srid);
            }
            GISEditorUtils.curRecentSRIDs();
            StringBuilder sridListStr = new StringBuilder();
            for (Integer sridInt : GISEditorUtils.getRecentSRIDs()) {
                if (sridListStr.length() > 0) sridListStr.append(",");
                sridListStr.append(sridInt);
            }
            GISViewerActivator.getDefault().getPreferences().setValue(PREF_RECENT_SRID_LIST, sridListStr.toString());
        }
        saveAttributeSettings();
    }

    @Override
    public void refresh() {
        try {
            reloadGeometryData(lastValue, true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Refresh", "Can't refresh value viewer", e);
        }
    }

    public void setGeometryData(@Nullable DBGeometry[] values) throws DBException {
        reloadGeometryData(values, false);
    }

    public void reloadGeometryData(@Nullable DBGeometry[] values, boolean force) throws DBException {
        if (!force && CommonUtils.equalObjects(lastValue, values)) {
            return;
        }
        int maxObjects = GISViewerActivator.getDefault().getPreferences().getInt(GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER);
        if (maxObjects <= 0) {
            maxObjects = GeometryViewerConstants.DEFAULT_MAX_OBJECTS_RENDER;
        }
        if (values != null && values.length > maxObjects) {
            // Truncate value list
            DBGeometry[] truncValues = new DBGeometry[maxObjects];
            System.arraycopy(values, 0, truncValues, 0, maxObjects);
            values = truncValues;
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

        int attributeSrid = GisConstants.SRID_SIMPLE;
        if (valueController != null && valueController.getValueType() instanceof GisAttribute) {
            try {
                attributeSrid = ((GisAttribute) valueController.getValueType())
                        .getAttributeGeometrySRID(new VoidProgressMonitor());
            } catch (DBCException e) {
                log.error(e);
            }
        }

        List<String> geomValues = new ArrayList<>();
        List<String> geomTipValues = new ArrayList<>();
        boolean showMap = false;
        for (int i = 0; i < values.length; i++) {
            DBGeometry value = values[i];
            if (DBUtils.isNullValue(value)) {
                continue;
            }
            if (flipCoordinates) {
                try {
                    value = value.flipCoordinates();
                } catch (DBException e) {
                    log.error(e);
                }
            }
            Object targetValue = value.getRawValue();
            int srid = sourceSRID == 0 ? value.getSRID() : sourceSRID;
            if (srid == GisConstants.SRID_SIMPLE) {
                srid = attributeSrid;
            }
            if (srid == GisConstants.SRID_SIMPLE) {
                showMap = false;
                actualSourceSRID = srid;
            } else if (srid == GisConstants.SRID_4326) {
                showMap = true;
                actualSourceSRID = srid;
            } else {
                Geometry geometry = value.getGeometry();
                if (geometry != null) {
                    try {
                        GisTransformRequest request = new GisTransformRequest(geometry, srid, GisConstants.SRID_4326);
                        GisTransformUtils.transformGisData(request);
                        targetValue = request.getTargetValue();
                        srid = request.getTargetSRID();
                        actualSourceSRID = request.getSourceSRID();
                        showMap = request.isShowOnMap();
                    } catch (DBException e) {
                        log.debug("Error transforming CRS", e);
                        actualSourceSRID = srid;
                        showMap = false;
                    }
                } else {
                    actualSourceSRID = srid;
                }
            }

            if (targetValue == null) {
                continue;
            }
            geomValues.add("'" + targetValue + "'");
            try {
                if (CommonUtils.isEmpty(value.getProperties())) {
                    geomTipValues.add("null");
                } else {
                    Map<String, Object> simplifiedProperties = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> pe : value.getProperties().entrySet()) {
                        Object pv = pe.getValue();
                        if (pv instanceof String || pv instanceof Number || pv instanceof Boolean || pv == null) {
                            // No changes
                        } else if (pv instanceof Map) {
                            simplifiedProperties.putAll((Map<? extends String, ?>) pv);
                        } else {
                            pv = CommonUtils.toString(pv);
                        }
                        simplifiedProperties.put(pe.getKey(), pv);
                    }
                    geomTipValues.add(gson.toJson(simplifiedProperties));
                }
            } catch (Exception e) {
                log.debug(e);
            }
        }
        this.defaultSRID = actualSourceSRID;
        String geomValuesString = String.join(",", geomValues);
        String geomTipValuesString = String.join(",", geomTipValues);
        String geomCRS = actualSourceSRID == GisConstants.SRID_SIMPLE ? GisConstants.LL_CRS_SIMPLE : GisConstants.LL_CRS_3857;
        boolean isShowMap = showMap;

        InputStream fis = GISViewerActivator.getDefault().getResourceStream(GISBrowserViewerConstants.VIEW_TEMPLATE_PATH);
        if (fis == null) {
            throw new IOException("View template file not found (" + GISBrowserViewerConstants.VIEW_TEMPLATE_PATH + ")");
        }
        try (InputStreamReader isr = new InputStreamReader(fis)) {
            String viewTemplate = IOUtils.readToString(isr);
            viewTemplate = GeneralUtils.replaceVariables(viewTemplate, name -> {
                switch (name) {
                    case "geomValues":
                        return geomValuesString;
                    case "geomTipValues":
                        return String.valueOf(geomTipValuesString);
                    case "geomSRID":
                        return String.valueOf(defaultSRID);
                    case "showMap":
                        return String.valueOf(isShowMap);
                    case "showTools":
                        return String.valueOf(toolsVisible);
                    case "geomCRS":
                        return geomCRS;
                    case "defaultTiles":
                        return GeometryViewerRegistry.getInstance().getDefaultLeafletTiles().getLayersDefinition();
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

    private void updateToolbar() {
        toolBarManager.removeAll();
        toolBarManager.add(new Action(GISMessages.panel_leaflet_viewer_tool_bar_action_text_open, DBeaverIcons.getImageDescriptor(UIIcon.BROWSER)) {
            @Override
            public void run() {
                UIUtils.launchProgram(scriptFile.getAbsolutePath());
            }
        });
        toolBarManager.add(new Action(GISMessages.panel_leaflet_viewer_tool_bar_action_text_copy_as, DBeaverIcons.getImageDescriptor(UIIcon.PICTURE)) {
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
        toolBarManager.add(new Action(GISMessages.panel_leaflet_viewer_tool_bar_action_text_save_as, DBeaverIcons.getImageDescriptor(UIIcon.PICTURE_SAVE)) {
            @Override
            public void run() {
                final Shell shell = browser.getShell();
                FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
                String[] extensions = new String[SUPPORTED_FORMATS.length];
                String[] filterNames = new String[SUPPORTED_FORMATS.length];
                for (int i = 0; i < SUPPORTED_FORMATS.length; i++) {
                    extensions[i] = "*." + SUPPORTED_FORMATS[i];
                    filterNames[i] = SUPPORTED_FORMATS[i].toUpperCase() + " (*." + SUPPORTED_FORMATS[i] + ")";
                }
                saveDialog.setFilterExtensions(extensions);
                saveDialog.setFilterNames(filterNames);
                String filePath = DialogUtils.openFileDialog(saveDialog);
                if (filePath == null) {
                    return;
                }
                int imageType = SWT.IMAGE_BMP;
                {
                    String filePathLower = filePath.toLowerCase();
                    if (filePathLower.endsWith(".png")) {
                        imageType = SWT.IMAGE_PNG;
                    } else if (filePathLower.endsWith(".gif")) {
                        imageType = SWT.IMAGE_GIF;
                    }
                }

                Image image = new Image(Display.getDefault(), browser.getBounds());
                GC gc = new GC(image);
                try {
                    browser.print(gc);
                } finally {
                    gc.dispose();
                }
                ImageLoader imageLoader = new ImageLoader();
                imageLoader.data = new ImageData[1];
                imageLoader.data[0] = image.getImageData();
                File outFile = new File(filePath);
                try (OutputStream fos = new FileOutputStream(outFile)) {
                    imageLoader.save(fos, imageType);
                } catch (IOException e) {
                    DBWorkbench.getPlatformUI().showError("Image save error", "Error saving as picture", e);
                }
                UIUtils.launchProgram(outFile.getAbsolutePath());
            }
        });

        toolBarManager.add(new Action(GISMessages.panel_leaflet_viewer_tool_bar_action_text_print, DBeaverIcons.getImageDescriptor(UIIcon.PRINT)) {
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

        toolBarManager.add(new Separator());

        Action crsSelectorAction = new SelectCRSAction(this);
        toolBarManager.add(ActionUtils.makeActionContribution(crsSelectorAction, true));

        Action tilesSelectorAction = new SelectTilesAction(this);
        toolBarManager.add(ActionUtils.makeActionContribution(tilesSelectorAction, true));

        toolBarManager.add(new Action(GISMessages.panel_leaflet_viewer_tool_bar_action_text_flip, Action.AS_CHECK_BOX) {
            {
                setToolTipText(GISMessages.panel_leaflet_viewer_tool_bar_action_tool_tip_text_flip);
                setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.LINK_TO_EDITOR));
            }

            @Override
            public boolean isChecked() {
                return flipCoordinates;
            }

            @Override
            public void run() {
                flipCoordinates = !flipCoordinates;
                try {
                    reloadGeometryData(lastValue, true);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Render error", "Error rendering geometry", e);
                }
                saveAttributeSettings();
                updateToolbar();
            }
        });

        toolBarManager.add(new Separator());

        toolBarManager.add(new Action(GISMessages.panel_leaflet_viewer_tool_bar_action_text_show_hide, Action.AS_CHECK_BOX) {
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

    private void saveAttributeSettings() {
        if (valueController instanceof IAttributeController) {
            DBDAttributeBinding binding = ((IAttributeController) valueController).getBinding();
            if (binding.getEntityAttribute() != null) {
                DBVEntity vEntity = DBVUtils.getVirtualEntity(binding, true);
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, true);
                if (vAttr != null) {
                    vAttr.setProperty(PROP_FLIP_COORDINATES, String.valueOf(flipCoordinates));
                    vAttr.setProperty(PROP_SRID, String.valueOf(getValueSRID()));
                }
                valueController.getExecutionContext().getDataSource().getContainer().getRegistry().flushConfig();
            }
        }
    }

    private void updateControlsVisibility() {
        GC gc = new GC(browser.getDisplay());
        try {
            browser.execute("javascript:showTools(" + toolsVisible +");");
        } finally {
            gc.dispose();
        }
    }

}