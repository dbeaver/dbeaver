/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Virtual model object
 */
public class DBVTransformSettings {
    private Set<String> excludedTransformers, includedTransformers;
    private String customTransformer;
    private Map<String, Object> transformOptions;

    DBVTransformSettings() {
    }

    DBVTransformSettings(DBVTransformSettings source) {
        this.excludedTransformers = source.excludedTransformers == null ? null : new HashSet<>(source.excludedTransformers);
        this.includedTransformers = source.includedTransformers == null ? null : new HashSet<>(source.includedTransformers);
        this.customTransformer = source.customTransformer;
        this.transformOptions = source.transformOptions == null ? null : new LinkedHashMap<>(source.transformOptions);
    }

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

    public void setCustomTransformer(String customTransformer) {
        this.customTransformer = customTransformer;
    }

    public Map<String, Object> getTransformOptions() {
        return transformOptions;
    }

    public void setTransformOption(String name, Object value) {
        if (this.transformOptions == null) {
            this.transformOptions = new LinkedHashMap<>();
        }
        this.transformOptions.put(name, value);
    }

    public void setTransformOptions(Map<String, Object> transformOptions) {
        this.transformOptions = transformOptions;
    }

    public boolean hasTransformOptions() {
        return transformOptions != null && !transformOptions.isEmpty();
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
            boolean valid;
            if (descriptor.isCustom()) {
                valid = descriptor.getId().equals(customTransformer);
            } else {
                if (descriptor.isApplicableByDefault()) {
                    valid = (excludedTransformers == null || !excludedTransformers.contains(descriptor.getId()));
                } else {
                    valid = includedTransformers != null && includedTransformers.contains(descriptor.getId());
                }
            }
            if (!valid) {
                descriptors.remove(i);
            } else {
                i++;
            }
        }
        return true;
    }


}
