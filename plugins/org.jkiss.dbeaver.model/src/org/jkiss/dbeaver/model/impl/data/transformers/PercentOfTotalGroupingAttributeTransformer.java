/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Transforms numeric values into their percentage of a total.
 */
public class PercentOfTotalGroupingAttributeTransformer implements DBDAttributeTransformer {

    public static final String TYPE_NAME = "percent";
    private static final Log log = Log.getLog(PercentOfTotalGroupingAttributeTransformer.class);
    private static final DecimalFormat df = new DecimalFormat("0.####%");


    private final TotalRowCountProvider totalRowCountSupplier;

    public PercentOfTotalGroupingAttributeTransformer(@NotNull TotalRowCountProvider totalRowCountSupplier) {
        this.totalRowCountSupplier = totalRowCountSupplier;
    }

    @Override
    public void transformAttribute(
        @NotNull DBCSession session,
        @NotNull DBDAttributeBinding attribute,
        @NotNull List<Object[]> rows,
        @NotNull Map<String, Object> options
    ) throws DBException {
        long totalRows = totalRowCountSupplier.getTotalRowCount(session.getProgressMonitor());
        attribute.setPresentationAttribute(new TransformerPresentationAttribute(attribute, TYPE_NAME, -1, DBPDataKind.NUMERIC));
        attribute.setTransformHandler(new PercentOfTotalValueHandler(attribute.getValueHandler(), totalRows));
    }

    private class PercentOfTotalValueHandler extends ProxyValueHandler {

        private final long total;

        public PercentOfTotalValueHandler(@NotNull DBDValueHandler target, long total) {
            super(target);
            if (total < 0) {
                throw new IllegalArgumentException("Total must be non-negative, but got: " + total);
            }
            this.total = total;
        }

        @Nullable
        @Override
        public Object getValueFromObject(
            @NotNull DBCSession session,
            @NotNull DBSTypedObject type,
            @Nullable Object object,
            boolean copy,
            boolean validateValue
        ) throws DBCException {
            if (object instanceof Number rowCount) {
                return percentOfTotal(rowCount.doubleValue(), total);
            }
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (value == null) {
                return "";
            }
            if (value instanceof Number rowCount) {
                return formatPercent(percentOfTotal(rowCount.doubleValue(), total));
            }
            log.trace("Unexpected value type for PercentOfTotalValueHandler: " + value.getClass().getName());
            return super.getValueDisplayString(column, value, format);
        }

        private double percentOfTotal(double value, double total) {
            if (total == 0) {
                return 0;
            }
            return value / total;
        }

        @NotNull
        private String formatPercent(double percent) {
            return df.format(percent);
        }
    }

    @FunctionalInterface
    public interface TotalRowCountProvider {
        long getTotalRowCount(@NotNull DBRProgressMonitor monitor) throws DBException;
    }
}
