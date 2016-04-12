/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Virtual model object
 */
public class DBVTransformSettings {
    private Set<String> excludedTransformers, includedTransformers;
    private String customTransformer;
    private Map<String, String> rendererProperties;

    public Set<String> getExcludedTransformers() {
        return excludedTransformers;
    }

    public boolean isExcluded(String id) {
        return excludedTransformers != null && excludedTransformers.contains(id);
    }

    public Set<String> getIncludedTransformers() {
        return includedTransformers;
    }

    public boolean isIncluded(String id) {
        return includedTransformers != null && includedTransformers.contains(id);
    }

    public void enableTransformer(DBDAttributeTransformerDescriptor transformer, boolean enable) {
        final String id = transformer.getId();
        if (includedTransformers == null) includedTransformers = new HashSet<>();
        if (excludedTransformers == null) excludedTransformers = new HashSet<>();
        if (enable) {
            if (!transformer.isApplicableByDefault()) {
                includedTransformers.add(id);
            }
            excludedTransformers.remove(id);
        } else {
            if (transformer.isApplicableByDefault()) {
                excludedTransformers.add(id);
            }
            includedTransformers.remove(id);
        }
    }

    public String getCustomTransformer() {
        return customTransformer;
    }

    public Map<String, String> getRendererProperties() {
        return rendererProperties;
    }

    public boolean hasValuableData() {
        return !CommonUtils.isEmpty(excludedTransformers) ||
            !CommonUtils.isEmpty(includedTransformers) ||
            !CommonUtils.isEmpty(customTransformer);
    }

    public boolean filterTransformers(List<? extends DBDAttributeTransformerDescriptor> descriptors) {
        if (!hasValuableData()) {
            return false;
        }
        for (int i = 0; i < descriptors.size();) {
            final DBDAttributeTransformerDescriptor descriptor = descriptors.get(i);
            if ((descriptor.isApplicableByDefault() && excludedTransformers != null && excludedTransformers.contains(descriptor.getId())) ||
                (!descriptor.isApplicableByDefault() && includedTransformers != null && !includedTransformers.contains(descriptor.getId())))
            {
                descriptors.remove(i);
            } else {
                i++;
            }
        }
        return true;
    }

}
