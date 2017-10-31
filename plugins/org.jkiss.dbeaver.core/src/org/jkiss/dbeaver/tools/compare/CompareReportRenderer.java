/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class CompareReportRenderer {

    private CompareReport report;
    private XMLBuilder xml;
    private CompareObjectsSettings settings;

    public void renderReport(DBRProgressMonitor monitor, CompareReport report, CompareObjectsSettings settings, OutputStream outputStream) throws IOException
    {
        this.report = report;
        this.settings = settings;
        this.xml = new XMLBuilder(outputStream, GeneralUtils.UTF8_ENCODING, true);
        this.xml.setButify(true);
        xml.addContent(
            "<!DOCTYPE html \n" +
            "     PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" +
            "    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

        if (settings.isShowOnlyDifferences()) {
            // Mark differences on tree nodes
            List<CompareReportLine> reportLines = report.getReportLines();
            int reportLinesSize = reportLines.size();
            for (int i = 0; i < reportLinesSize; i++) {
                if (reportLines.get(i).hasDifference) {
                    int depth = reportLines.get(i).depth;
                    for (int k = i - 1; k >= 0; k--) {
                        CompareReportLine prevNode = reportLines.get(k);
                        if (prevNode.depth < depth) {
                            if (prevNode.hasDifference) {
                                // Already set
                                break;
                            }
                            depth = prevNode.depth;
                            prevNode.hasDifference = true;
                        }
                    }
                }
            }
        }

        xml.startElement("html");
        xml.startElement("head");
        xml.startElement("meta");
        xml.addAttribute("http-equiv", "Content-type");
        xml.addAttribute("content", "text/html; charset=utf-8");
        xml.endElement();
        xml.startElement("title");
        xml.addText("Compare report");
        xml.endElement();
        xml.endElement();
        xml.startElement("body");

        renderHeader();

        xml.startElement("table");
        xml.addAttribute("width", "100%");
        //xml.addAttribute("border", "1");
        xml.addAttribute("cellspacing", 0);
        xml.addAttribute("cellpadding", 0);
        renderBody(monitor);

        xml.endElement();
        xml.endElement();
        xml.endElement();

        this.xml.flush();
    }

    private void renderHeader() throws IOException
    {
        int maxLevel = 0;
        for (CompareReportLine line : report.getReportLines()) {
            if (line.depth > maxLevel) {
                maxLevel = line.depth;
            }
        }
        maxLevel++;
        xml.startElement("style");
        StringBuilder styles = new StringBuilder();
        styles.append("table {font-family:\"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;font-size:12px;text-align:left;} ");
        styles.append(".missing {color:red;} .differs {color:red;} ");
        styles.append(".object td,th {border-top:solid 1px; border-right:solid 1px; border-color: black; white-space:nowrap;} ");
        styles.append(".property td,th {border-right:solid 1px; border-color: black; white-space:pre; } ");
        styles.append(".struct {border-top:none; !important } ");
//        styles.append(".object:first-child {border:none; } ");
//        styles.append(".property:first-child {border:none; } ");
        for (int i = 1; i <= maxLevel; i++) {
            styles.append(".level").append(i).append(" td,th { text-align:left; padding-left:").append(20 * i).append("px; } ");
        }
        xml.addText(styles.toString(), false);
        xml.endElement();
    }

    private void renderBody(DBRProgressMonitor monitor) throws IOException
    {
        // Table head
        xml.startElement("tr");
        xml.startElement("th");
        xml.addText("Structure");
        xml.endElement();
        for (DBNDatabaseNode node : report.getNodes()) {
            xml.startElement("th");
            xml.addText(node.getNodeFullName());
            xml.endElement();
        }
        xml.endElement();

        // Table body
        boolean showOnlyDifferences = settings.isShowOnlyDifferences();
        int objectCount = report.getNodes().size();
        List<CompareReportLine> reportLines = report.getReportLines();
        int reportLinesSize = reportLines.size();
        for (int i = 0; i < reportLinesSize; i++) {
            monitor.worked(1);
            CompareReportLine line = reportLines.get(i);
            if (showOnlyDifferences && !line.hasDifference) {
                continue;
            }
            boolean onlyStructure = line.structure instanceof DBNDatabaseFolder && !line.hasDifference;
            // Skip empty folders
            if (onlyStructure && (i >= reportLinesSize - 1 || reportLines.get(i + 1).depth <= line.depth)) {
                continue;
            }

            xml.startElement("tr");
            xml.addAttribute("class", "object level" + line.depth);
            xml.startElement("td");
            xml.addText(line.structure.getNodeType());
            xml.endElement();
            if (onlyStructure) {
                xml.startElement("td");
                xml.addAttribute("colspan", line.nodes.length);
                xml.addText("&nbsp;", false);
                xml.endElement();
            } else {
                for (int k = 0; k < objectCount; k++) {
                    xml.startElement("td");
                    if (line.nodes[k] == null) {
                        xml.addAttribute("class", "missing");
                        xml.addText("N/A");
                    } else {
                        xml.addText(line.nodes[k].getName());
                    }
                    xml.endElement();
                }
            }

            xml.endElement();

            if (line.properties != null) {
                for (CompareReportProperty reportProperty : line.properties) {
                    boolean differs = false;
                    Object firstValue = null;
                    boolean hasValue = false;
                    for (int k = 0; k < reportProperty.values.length; k++) {
                        if (line.nodes[k] == null) {
                            // Ignore properties of missing objects
                            continue;
                        }
                        Object value = reportProperty.values[k];
                        if (value != null) {
                            hasValue = true;
                            if (firstValue == null) {
                                firstValue = value;
                            }
                        }
                        if (!CompareUtils.equalPropertyValues(value, firstValue)) {
                            differs = true;
                            break;
                        }
                    }
                    if (!hasValue) {
                        // Skip[ properties when nobody have it's value
                        continue;
                    }
                    if (showOnlyDifferences && !differs) {
                        continue;
                    }
                    xml.startElement("tr");
                    xml.addAttribute("class", "property level" + (line.depth + 1) + (differs ? " differs" : ""));
                    xml.startElement("td");
                    xml.addText(reportProperty.property.getDisplayName());
                    xml.endElement();

                    for (int k = 0; k < objectCount; k++) {
                        xml.startElement("td");
                        String stringValue = "";
                        if (reportProperty.values[k] != null) {
                            stringValue = reportProperty.values[k].toString();
                        }
                        if (CommonUtils.isEmpty(stringValue)) {
                            xml.addText("&nbsp;", false);
                        } else {
                            xml.addText(stringValue);
                        }

                        xml.endElement();
                    }

                    xml.endElement();
                }
            }
        }

        // Table footer
        xml.startElement("tr");
        xml.addAttribute("class", "object");
        xml.startElement("td");
        xml.addAttribute("colspan", report.getNodes().size() + 1);
        xml.addText("" + reportLines.size() + " objects compared");
        xml.endElement();
        xml.endElement();

    }
}