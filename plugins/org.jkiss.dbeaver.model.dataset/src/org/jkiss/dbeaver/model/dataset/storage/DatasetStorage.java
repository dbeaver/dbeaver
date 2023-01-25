/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.dataset.storage;

import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.dataset.DBDDataSet;
import org.jkiss.dbeaver.model.dataset.DBDDataSetQuery;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Dataset storage
 */
public class DatasetStorage {
    private static final String TAG_DATASET = "dataset"; //NON-NLS-1
    private static final String TAG_QUERIES = "queries"; //NON-NLS-1
    private static final String TAG_QUERY = "query"; //NON-NLS-1
    private static final String TAG_FILTERS = "filters"; //NON-NLS-1
    private static final String TAG_CONSTRAINT = "constraint"; //NON-NLS-1

    private static final String ATTR_ID = "id"; //NON-NLS-1
    private static final String ATTR_NAME = "name"; //NON-NLS-1
    private static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    private static final String ATTR_QUERY_TEXT = "queryText"; //NON-NLS-1
    private static final String ATTR_DRAFT = "draft"; //NON-NLS-1
    private static final String ATTR_DATA_SOURCE = "data-source"; //NON-NLS-1
    private static final String ATTR_CATALOG = "catalog"; //NON-NLS-1
    private static final String ATTR_SCHEMA = "schema"; //NON-NLS-1

    private static final String ATTR_ORDER = "order"; //NON-NLS-1
    private static final String ATTR_WHERE = "where"; //NON-NLS-1
    private static final String ATTR_ANY_CONSTRAINT = "anyConstraint"; //NON-NLS-1
    private static final String ATTR_LABEL = "attributeLabel"; //NON-NLS-1
    private static final String ATTR_PLAIN_NAME = "plainNameReference"; //NON-NLS-1
    private static final String ATTR_VISUAL_POSITION = "originalVisualPosition"; //NON-NLS-1
    private static final String ATTR_ORDER_POSITION = "originalVisualPosition"; //NON-NLS-1
    private static final String ATTR_ORDER_DESCENDING = "orderDescending"; //NON-NLS-1
    private static final String ATTR_CRITERIA = "criteria"; //NON-NLS-1
    private static final String ATTR_OPERATOR = "operator"; //NON-NLS-1
    private static final String ATTR_REVERSE_OPERATOR = "reverseOperator"; //NON-NLS-1

    private final DBDDataSet dataSet;

    public DatasetStorage(DBDDataSet dataSet) {
        this.dataSet = dataSet;
    }

    public DatasetStorage(String datasetId,
                          byte[] xmlData,
                          DBPDataSourceRegistry dataSourceRegistry
    ) throws DBException, CoreException {
        try (InputStream contents = new ByteArrayInputStream(xmlData)) {
            final Document document = XMLUtils.parseDocument(contents);
            final Element root = document.getDocumentElement();

            dataSet = new DBDDataSet(
                datasetId,
                root.getAttribute(ATTR_NAME)
            );
            dataSet.setDescription(root.getAttribute(ATTR_DESCRIPTION));
            dataSet.setDraft(CommonUtils.toBoolean(root.getAttribute(ATTR_DRAFT)));
            deserializeQueries(root, dataSet, dataSourceRegistry);
        } catch (Exception e) {
            throw new DBException("I>O Error reading dataset '" + datasetId + "'", e);
        }
    }

    private void deserializeQueries(
        Element root,
        DBDDataSet dataSet,
        DBPDataSourceRegistry dataSourceRegistry
    ) throws DBException {
        Element queriesElement = getSingleChildElement(root, TAG_QUERIES);
        if (queriesElement == null) {
            return;
        }
        NodeList allQueries = queriesElement.getElementsByTagName(TAG_QUERY);
        for (int i = 0; i < allQueries.getLength(); i++) {
            Element queryElement = (Element) allQueries.item(i);
            dataSet.addQuery(deserializeQuery(queryElement, dataSourceRegistry));
        }
    }

    @Nullable
    private Element getSingleChildElement(Element root, String tagName) {
        var elementsList = root.getElementsByTagName(tagName);
        if (elementsList.getLength() == 0) {
            return null;
        }
        if (elementsList.getLength() > 1) {
            try {
                throw new DBException(
                    "Error reading dataset: dataset contains multiple '" + tagName + "'  tag"
                );
            } catch (DBException e) {
                throw new RuntimeException(e);
            }
        }

        return (Element) elementsList.item(0);
    }

    private DBDDataSetQuery deserializeQuery(Element queryElement, DBPDataSourceRegistry dataSourceRegistry) {
        DBDDataSetQuery query = new DBDDataSetQuery(queryElement.getAttribute(ATTR_ID));
        query.setDescription(CommonUtils.nullIfEmpty(queryElement.getAttribute(ATTR_DESCRIPTION)));
        query.setQueryText(queryElement.getAttribute(ATTR_QUERY_TEXT));
        query.setDataSourceContainer(dataSourceRegistry.getDataSource(queryElement.getAttribute(ATTR_DATA_SOURCE)));
        query.setCatalog(CommonUtils.nullIfEmpty(queryElement.getAttribute(ATTR_CATALOG)));
        query.setSchema(CommonUtils.nullIfEmpty(queryElement.getAttribute(ATTR_SCHEMA)));

        var filtersElement = getSingleChildElement(queryElement, TAG_FILTERS);
        if (filtersElement != null) {
            DBDDataFilter filter = deserializeFilter(filtersElement);
            query.setDataFilters(filter);
        }
        return query;
    }


    public DBDDataSet getDataSet() {
        return dataSet;
    }

    public ByteArrayInputStream serialize() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(5000);
        XMLBuilder xml = new XMLBuilder(buffer, GeneralUtils.getDefaultFileEncoding());
        xml.startElement(TAG_DATASET);
        xml.addAttribute(ATTR_NAME, dataSet.getDisplayName());
        if (dataSet.getDescription() != null) {
            xml.addAttribute(ATTR_DESCRIPTION, dataSet.getDescription());
        }
        xml.addAttribute(ATTR_DRAFT, String.valueOf(dataSet.isDraft()));

        try (var queriesXmlElement = xml.startElement(TAG_QUERIES)) {
            for (DBDDataSetQuery query : dataSet.getQueries()) {
                serializeQuery(xml, query);
            }
        }

        xml.endElement();
        xml.flush();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    private void serializeQuery(XMLBuilder xml, DBDDataSetQuery query) throws IOException {
        try (var queryXmlElement = xml.startElement(TAG_QUERY)) {
            xml.addAttribute(ATTR_ID, query.getId());
            xml.addAttribute(ATTR_DATA_SOURCE, query.getDataSourceContainer().getId());
            xml.addAttribute(ATTR_QUERY_TEXT, query.getQueryText());
            addAttributeIfNotEmpty(xml, ATTR_CATALOG, query.getCatalog());
            addAttributeIfNotEmpty(xml, ATTR_SCHEMA, query.getSchema());
            addAttributeIfNotEmpty(xml, ATTR_DESCRIPTION, query.getDescription());
            serializeQueryFilters(xml, query.getDataFilters());
        }
    }

    private void serializeQueryFilters(XMLBuilder xml, DBDDataFilter dataFilters) throws IOException {
        if (dataFilters == null || !dataFilters.hasFilters()) {
            // the filter is empty and we don't need to serialize it
            return;
        }
        try (var filtersXmlElement = xml.startElement(TAG_FILTERS)) {
            addAttributeIfNotEmpty(xml, ATTR_ORDER, dataFilters.getOrder());
            addAttributeIfNotEmpty(xml, ATTR_WHERE, dataFilters.getWhere());
            addAttributeIfNotEmpty(xml, ATTR_ANY_CONSTRAINT, String.valueOf(dataFilters.isAnyConstraint()));
            for (DBDAttributeConstraint constraint : dataFilters.getConstraints()) {
                serializeFilterConstraint(xml, constraint);
            }
        }
    }

    private DBDDataFilter deserializeFilter(Element filtersElement) {
        DBDDataFilter filter = new DBDDataFilter();
        filter.setOrder(CommonUtils.nullIfEmpty(filtersElement.getAttribute(ATTR_ORDER)));
        filter.setWhere(CommonUtils.nullIfEmpty(filtersElement.getAttribute(ATTR_WHERE)));
        filter.setAnyConstraint(CommonUtils.toBoolean(filtersElement.getAttribute(ATTR_ANY_CONSTRAINT)));
        NodeList constraints = filtersElement.getElementsByTagName(TAG_CONSTRAINT);
        for (int i = 0; i < constraints.getLength(); i++) {
            Element constraintElement = (Element) constraints.item(i);
            DBDAttributeConstraint constraint = new DBDAttributeConstraint(
                constraintElement.getAttribute(ATTR_NAME),
                constraintElement.getAttribute(ATTR_LABEL),
                CommonUtils.toInt(constraintElement.getAttribute(ATTR_VISUAL_POSITION))
            );
            constraint.setPlainNameReference(CommonUtils.toBoolean(constraintElement.getAttribute(ATTR_PLAIN_NAME)));
            constraint.setOrderPosition(CommonUtils.toInt(constraintElement.getAttribute(ATTR_ORDER_POSITION)));
            constraint.setOrderDescending(CommonUtils.toBoolean(constraintElement.getAttribute(ATTR_ORDER_DESCENDING)));
            constraint.setCriteria(CommonUtils.nullIfEmpty(constraintElement.getAttribute(ATTR_CRITERIA)));

            var operatorId = CommonUtils.nullIfEmpty(constraintElement.getAttribute(ATTR_OPERATOR));
            if (operatorId != null) {
                constraint.setOperator(DBCLogicalOperator.valueOf(operatorId));
                constraint.setReverseOperator(CommonUtils.toBoolean(constraintElement.getAttribute(ATTR_REVERSE_OPERATOR)));
            }
            filter.addConstraints(List.of(constraint));
        }

        return filter;
    }

    private void serializeFilterConstraint(XMLBuilder xml, DBDAttributeConstraint constraint) throws IOException {
        try (var constraintXmlElement = xml.startElement(TAG_CONSTRAINT)) {
            addAttributeIfNotEmpty(xml, ATTR_NAME, constraint.getAttributeName());
            addAttributeIfNotEmpty(xml, ATTR_VISUAL_POSITION, String.valueOf(constraint.getOriginalVisualPosition()));
            addAttributeIfNotEmpty(xml, ATTR_LABEL, constraint.getAttributeLabel());
            addAttributeIfNotEmpty(xml, ATTR_PLAIN_NAME, String.valueOf(constraint.isPlainNameReference()));
            addAttributeIfNotEmpty(xml, ATTR_ORDER_POSITION, String.valueOf(constraint.getOrderPosition()));
            addAttributeIfNotEmpty(xml, ATTR_ORDER_DESCENDING, String.valueOf(constraint.isOrderDescending()));
            addAttributeIfNotEmpty(xml, ATTR_CRITERIA, constraint.getCriteria());
            if (constraint.getOperator() != null) {
                addAttributeIfNotEmpty(xml, ATTR_OPERATOR, constraint.getOperator().getId());
                addAttributeIfNotEmpty(xml, ATTR_REVERSE_OPERATOR, String.valueOf(constraint.isReverseOperator()));
            }
        }
    }

    private void addAttributeIfNotEmpty(XMLBuilder xmlBuilder, String attrName, String attrValue) throws IOException {
        if (CommonUtils.isEmpty(attrValue)) {
            return;
        }

        xmlBuilder.addAttribute(attrName, attrValue);
    }
}
