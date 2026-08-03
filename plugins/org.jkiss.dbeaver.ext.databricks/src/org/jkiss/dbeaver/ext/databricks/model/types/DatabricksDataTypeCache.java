/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.databricks.model.types;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.databricks.DatabricksDataTypesLexer;
import org.jkiss.dbeaver.ext.databricks.DatabricksDataTypesParser;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class DatabricksDataTypeCache extends GenericDataTypeCache {

    @NotNull
    private static final Log log = Log.getLog(DatabricksDataTypeCache.class);

    @NotNull
    private final Map<String, DatabricksDataType> trivialTypeByName = new HashMap<>();

    @NotNull
    private final Map<String, DatabricksDataType> explicitTypeBySpec = new WeakHashMap<>();

    public DatabricksDataTypeCache(@NotNull GenericStructContainer container) {
        super(container);
    }

    @Nullable
    @Override
    public GenericDataType getCachedObject(@Nullable String name) {
        DatabricksDataType type = name == null ? null : this.resolveBuiltinDataType(name);
        return type != null ? type : super.getCachedObject(name);
    }

    @Nullable
    private DatabricksDataType resolveBuiltinDataType(@NotNull String typeFullName) {
        return this.explicitTypeBySpec.computeIfAbsent(typeFullName, this::parseBuiltinDataType);
    }

    @Nullable
    private DatabricksDataType parseBuiltinDataType(@NotNull String typeSpec) {
        try {
            var lexer = new DatabricksDataTypesLexer(CharStreams.fromString(typeSpec));
            var parser = new DatabricksDataTypesParser(new CommonTokenStream(lexer));
            return recognizeDataType(parser.anyType());
        } catch (Throwable ex) {
            log.debug(ex);
            return null;
        }
    }

    @Nullable
    private DatabricksDataType recognizeDataType(@Nullable ParseTree node) {
        return switch (node) {
            case null -> null;
            case DatabricksDataTypesParser.BigIntTypeContext n -> this.obtainTrivialType(n, Types.BIGINT, "BIGINT");
            case DatabricksDataTypesParser.BinaryTypeContext n -> this.obtainTrivialType(n, Types.BINARY, "BINARY");
            case DatabricksDataTypesParser.DateTypeContext n -> this.obtainTrivialType(n, Types.DATE, "DATE");
            case DatabricksDataTypesParser.DoubleTypeContext n -> this.obtainTrivialType(n, Types.DOUBLE, "DOUBLE");
            case DatabricksDataTypesParser.FloatTypeContext n -> this.obtainTrivialType(n, Types.FLOAT, "FLOAT");
            case DatabricksDataTypesParser.IntTypeContext n -> this.obtainTrivialType(n, Types.INTEGER, "INT");
            case DatabricksDataTypesParser.SmallintTypeContext n -> this.obtainTrivialType(n, Types.SMALLINT, "SMALLINT");
            case DatabricksDataTypesParser.StringTypeContext n -> this.obtainTrivialType(n, Types.VARCHAR, "STRING");
            case DatabricksDataTypesParser.TimestampTypeContext n -> this.obtainTrivialType(n, Types.TIMESTAMP, "TIMESTAMP");
            case DatabricksDataTypesParser.TimestampNtzTypeContext n -> this.obtainTrivialType(n, Types.TIMESTAMP, "TIMESTAMP_NTZ");
            case DatabricksDataTypesParser.TinyIntTypeContext n -> this.obtainTrivialType(n, Types.TINYINT, "TINYINT");
            case DatabricksDataTypesParser.VariantTypeContext n -> this.obtainTrivialType(n, Types.OTHER, "VARIANT");
            case DatabricksDataTypesParser.VoidTypeContext n -> this.obtainTrivialType(n, Types.OTHER, "VOID");

            case DatabricksDataTypesParser.GeographyTypeContext n -> this.recognizeSridBasedTypeSpec(n, "GEOGRAPHY");
            case DatabricksDataTypesParser.GeometryTypeContext n -> this.recognizeSridBasedTypeSpec(n, "GEOMETRY");

            case DatabricksDataTypesParser.DecimalTypeContext n -> this.recognizeDecimalTypeSpec(n);
            case DatabricksDataTypesParser.IntervalTypeContext n -> new DatabricksDataType(this.owner, Types.OTHER, n.getText(), 0, 0);
            case DatabricksDataTypesParser.TimeTypeContext n -> this.recognizeTimeTypeSpec(n, n.getText());
            case DatabricksDataTypesParser.ArrayTypeContext n -> this.recognizeArrayTypeSpec(n);
            case DatabricksDataTypesParser.MapTypeContext n -> this.recognizeMapTypeSpec(n);
            case DatabricksDataTypesParser.StructTypeContext n -> this.recognizeStructTypeSpec(n, false);
            case DatabricksDataTypesParser.ObjectTypeContext n -> this.recognizeStructTypeSpec(n, true);

            default -> node.getChildCount() == 1 ? recognizeDataType(node.getChild(0)) : null;
        };
    }

    @NotNull
    private DatabricksDataType obtainTrivialType(@NotNull ParseTree node, int jdbcTypeKind, @NotNull String name) {
        return this.trivialTypeByName.computeIfAbsent(name, s -> new DatabricksDataType(this.owner, jdbcTypeKind, s, 0, 0));
    }

    @Nullable
    private DatabricksDataType recognizeSridBasedTypeSpec(@NotNull ParseTree node, @NotNull String name) {
        // name(srid)
        if (node.getChildCount() < 3) {
            return null;
        } else {
            String sridSpec = node.getChild(2).getText();
            return new DatabricksDataType(this.owner, name, sridSpec.equalsIgnoreCase("ANY") ? 0 : Integer.parseInt(sridSpec));
        }
    }

    @Nullable
    private DatabricksDataType recognizeDecimalTypeSpec(@NotNull ParseTree node) {
        // { DECIMAL | DEC | NUMERIC } [ (  p [ , s ] ) ]
        String name = "DECIMAL";
        DatabricksDataType result;
        if (node.getChildCount() < 1) {
            result = null;
        } else if (node.getChildCount() == 1) {
            result = this.trivialTypeByName.computeIfAbsent(name, s -> new DatabricksDataType(this.owner, Types.DECIMAL, name, 10, 0));
        } else {
            int precision = node.getChildCount() > 1 ? Integer.parseInt(node.getChild(2).getText()) : 10;
            int scale =  node.getChildCount() > 4 ? Integer.parseInt(node.getChild(4).getText()) : 0;
            result = new DatabricksDataType(this.owner, Types.DECIMAL, name, precision, scale);
        }
        return result;
    }

    @Nullable
    private DatabricksDataType recognizeTimeTypeSpec(@NotNull ParseTree node, @NotNull String name) {
        // TIME | TIME(p)
        DatabricksDataType result;
        if (node.getChildCount() < 1) {
            result = null;
        } else if (node.getChildCount() == 1) {
            result = this.trivialTypeByName.computeIfAbsent(name, s -> new DatabricksDataType(this.owner, Types.TIME, name, 0, 0));
        } else {
            result = new DatabricksDataType(this.owner, Types.TIME, name, Integer.parseInt(node.getChild(2).getText()), 0);
        }
        return result;
    }

    @Nullable
    private DatabricksDataType recognizeArrayTypeSpec(@NotNull ParseTree node) {
        if (node.getChildCount() < 3) {
            return null;
        } else {
            DatabricksDataType itemType = this.recognizeDataType(node.getChild(2));
            return itemType == null ? null : (
                itemType instanceof DBSEntity e
                    ?  new DatabricksArrayOfEntitiesDataType(this.owner, e)
                    :  new DatabricksArrayDataType(this.owner, itemType)
            );
        }
    }

    @Nullable
    private DatabricksDataType recognizeStructTypeSpec(@NotNull ParseTree node, boolean isObject) {
        Map<String, DatabricksDataType> members = new LinkedHashMap<>();

        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChild(i) instanceof RuleContext e) {
                DatabricksDataType memberType = this.recognizeDataType(e.getChild(node.getChildCount() > 2 ? 2 : 1));
                members.put(e.getChild(0).getText(), memberType);
            }
        }

        return new DatabricksStructDataType(this.owner, members, isObject);
    }

    @Nullable
    private DatabricksDataType recognizeMapTypeSpec(@NotNull ParseTree node) {
        if (node.getChildCount() < 5) {
            return null;
        } else {
            DatabricksDataType keyType = this.recognizeDataType(node.getChild(2));
            DatabricksDataType valueType = this.recognizeDataType(node.getChild(4));
            if (keyType != null && valueType != null) {
                return new DatabricksMapDataType(this.owner, keyType, valueType);
            } else {
                throw new IllegalStateException("Unsupported map-like type spec " + node.getText());
            }
        }
    }
}
