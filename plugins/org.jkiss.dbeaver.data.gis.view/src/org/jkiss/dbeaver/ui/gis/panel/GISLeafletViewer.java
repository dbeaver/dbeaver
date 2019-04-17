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
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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
    private File scriptFile;

    public GISLeafletViewer(Composite parent, IValueController valueController) {
        browser = new Browser(parent, SWT.NONE);
        browser.addDisposeListener(e -> {
            cleanupFiles();
        });
        this.valueController = valueController;
    }

    public void setGeometryData(@Nullable DBGeometry[] values) throws DBException
    {
        if (CommonUtils.equalObjects(lastValue, values)) {
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
            int srid = value.getSRID();
            if (srid == 0) {
                srid = GisConstants.DEFAULT_SRID;
            } else {
                if (baseSRID == 0) {
                    baseSRID = srid;
                }
            }
            if (srid == GisConstants.DEFAULT_SRID) {
                showMap = true;
            } else {
                Geometry geometry = value.getGeometry();
                if (geometry != null) {
                    try {
                        GisTransformRequest request = new GisTransformRequest(geometry, srid, GisConstants.DEFAULT_SRID);
                        GisTransformUtils.transformGisData(request);
                        targetValue = request.getTargetValue();
                        showMap = request.isShowOnMap();
                    } catch (DBException e) {
                        log.debug("Error transforming CRS", e);
                        showMap = false;
                    }
                } else {
                    showMap = false;
                }
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
        int defaultSRID = baseSRID;
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

    public Browser getBrowser() {
        return browser;
    }

    public DBGeometry[] getCurrentValue() {
        return lastValue;
    }
}