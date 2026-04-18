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
package org.jkiss.dbeaver.test.platform;

import org.jkiss.dbeaver.data.transfer.StreamTransferUtilsTest;
import org.jkiss.dbeaver.model.DBUtilsTest;
import org.jkiss.dbeaver.model.DBValueFormattingTest;
import org.jkiss.dbeaver.model.DataTypeConverterTest;
import org.jkiss.dbeaver.model.SQLUtilsTest;
import org.jkiss.dbeaver.model.data.aggregate.FunctionCountNullsTest;
import org.jkiss.dbeaver.model.data.json.JSONUtilsTest;
import org.jkiss.dbeaver.model.impl.DefaultCertificateStorageTest;
import org.jkiss.dbeaver.model.impl.data.transformers.EpochTimeAttributeTransformerTest;
import org.jkiss.dbeaver.model.impl.jdbc.DatabaseURLTest;
import org.jkiss.dbeaver.model.sql.AbstractSQLDialectTest;
import org.jkiss.dbeaver.model.sql.analyzer.SQLCompletionAnalyzerTest;
import org.jkiss.dbeaver.model.sql.analyzer.SQLQueryCompletionAnalyzerTest;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLFormatterTokenizedTest;
import org.jkiss.dbeaver.model.sql.parser.DiffTest;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParserGenericsTest;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParserTest;
import org.jkiss.dbeaver.model.sql.parser.TokenPredicatesConditionTest;
import org.jkiss.dbeaver.model.sql.parser.TrieTest;
import org.jkiss.dbeaver.tools.transfer.CSVImporterTest;
import org.jkiss.dbeaver.tools.transfer.DataExporterCSVTest;
import org.jkiss.dbeaver.ui.svg.SVGTest;
import org.jkiss.util.AlphanumericComparatorTest;
import org.jkiss.utils.ArrayUtilsTest;
import org.jkiss.utils.BOMInputStreamTest;
import org.jkiss.utils.BeanUtilsTest;
import org.jkiss.utils.ByteNumberFormatTest;
import org.jkiss.utils.CommonUtilsTest;
import org.jkiss.utils.DurationFormatterTest;
import org.jkiss.utils.GeneralUtilsTest;
import org.jkiss.utils.IOUtilsTest;
import org.jkiss.utils.MimeTypeTest;
import org.jkiss.utils.RestTest;
import org.jkiss.utils.RuntimeUtilsTest;
import org.jkiss.utils.SecurityUtilsTest;
import org.jkiss.utils.StringUtilsTest;
import org.jkiss.utils.time.ExtendedDateFormatTest;
import org.jkiss.junit.ApplicationUnitTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    AlphanumericComparatorTest.class,
    SVGTest.class,
    CSVImporterTest.class,
    DataExporterCSVTest.class,
    PlatformTest.class,
    DatabaseURLTest.class,
    DefaultCertificateStorageTest.class,
    EpochTimeAttributeTransformerTest.class,
    DataTypeConverterTest.class,
    DBUtilsTest.class,
    DBValueFormattingTest.class,
    SQLUtilsTest.class,
    FunctionCountNullsTest.class,
    JSONUtilsTest.class,
    SQLScriptParserGenericsTest.class,
    TokenPredicatesConditionTest.class,
    TrieTest.class,
    DiffTest.class,
    SQLScriptParserTest.class,
    AbstractSQLDialectTest.class,
    SQLFormatterTokenizedTest.class,
    SQLCompletionAnalyzerTest.class,
    SQLQueryCompletionAnalyzerTest.class,
    StreamTransferUtilsTest.class,
    BOMInputStreamTest.class,
    BeanUtilsTest.class,
    IOUtilsTest.class,
    ByteNumberFormatTest.class,
    MimeTypeTest.class,
    RuntimeUtilsTest.class,
    RestTest.class,
    DurationFormatterTest.class,
    SecurityUtilsTest.class,
    ExtendedDateFormatTest.class,
    GeneralUtilsTest.class,
    StringUtilsTest.class,
    ArrayUtilsTest.class,
    CommonUtilsTest.class
})
public class DBeaerTestPlatformSuite extends ApplicationUnitTest {
}
