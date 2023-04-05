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
package org.jkiss.dbeaver.antlr.sql.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.jkiss.dbeaver.antlr.model.SyntaxModel;
import org.jkiss.dbeaver.antlr.sql.Sql92Lexer;
import org.jkiss.dbeaver.antlr.sql.Sql92Parser;
import org.jkiss.dbeaver.antlr.sql.model.SelectStatement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ParseSelectStmtTest {
    
    private static final String _selectStatementsSqlTextResourceName = "SelectStatements.sql.txt";
    
    @Test
    public void testModel() throws IOException {
        var inputStream = ParseSelectStmtTest.class.getResourceAsStream(_selectStatementsSqlTextResourceName);
        var input = CharStreams.fromStream(inputStream);
        var ll = new Sql92Lexer(input);
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        
        var pp = new Sql92Parser(tokens);
        pp.setBuildParseTree(true);
        
        var tree = pp.queryExpression();
        
        SyntaxModel model = new SyntaxModel(pp);
        model.introduce(SelectStatement.class);
        var result = model.map(tree, SelectStatement.class);
        
        Assert.assertTrue(result.isOk());
        System.out.println(model.stringify(result.model));
    }

    
            /*
                SELECT TOP 25
                    Product.ProductID,
                    Product.Name AS ProductName,
                    Product.ProductNumber,
                    CostMeasure.UnitMeasureCode,
                    CostMeasure.Name AS CostMeasureName,
                    ProductVendor.AverageLeadTime,
                    ProductVendor.StandardPrice,
                    ProductReview.ReviewerName,
                    ProductReview.Rating,
                    ProductCategory.Name AS CategoryName,
                    ProductSubCategory.Name AS SubCategoryName
                FROM Production.Product
                INNER JOIN Production.ProductSubCategory
                ON ProductSubCategory.ProductSubcategoryID = Product.ProductSubcategoryID
                LEFT JOIN Production.ProductCategory
                ON ProductCategory.ProductCategoryID = ProductSubCategory.ProductCategoryID
                RIGHT JOIN Production.UnitMeasure SizeUnitMeasureCode
                ON Product.SizeUnitMeasureCode = SizeUnitMeasureCode.UnitMeasureCode
                FULL JOIN Production.UnitMeasure WeightUnitMeasureCode
                ON Product.WeightUnitMeasureCode = WeightUnitMeasureCode.UnitMeasureCode
                INNER OUTER JOIN Production.ProductModel
                ON ProductModel.ProductModelID = Product.ProductModelID
                NATURAL LEFT OUTER JOIN Production.ProductModelIllustration
                ON ProductModel.ProductModelID = ProductModelIllustration.ProductModelID
                NATURAL UNION JOIN Production.ProductModelProductDescriptionCulture
                ON ProductModelProductDescriptionCulture.ProductModelID = ProductModel.ProductModelID
                NATURAL FULL OUTER JOIN Production.ProductDescription
                ON ProductDescription.ProductDescriptionID = ProductModelProductDescriptionCulture.ProductDescriptionID
                CROSS JOIN Production.ProductReview
                ON ProductReview.ProductID = Product.ProductID
                NATURAL INNER JOIN Purchasing.ProductVendor
                ON ProductVendor.ProductID = Product.ProductID
                LEFT JOIN Production.UnitMeasure CostMeasure
                USING (MeasureID, Unit)
                ORDER BY Product.ProductID DESC;

                SELECT TOP 25
                    Product.ProductID,
                    Product.Name AS ProductName,
                    Product.ProductNumber,
                    ProductCategory.Name AS ProductCategory,
                    ProductSubCategory.Name AS ProductSubCategory,
                    Product.ProductModelID
                INTO Product
                FROM Production.Product
                INNER JOIN Production.ProductSubCategory
                ON ProductSubCategory.ProductSubcategoryID = Product.ProductSubcategoryID
                UNION JOIN Production.ProductCategory
                ON ProductCategory.ProductCategoryID = ProductSubCategory.ProductCategoryID
                ORDER BY Product.ModifiedDate DESC;


            SELECT
                    Product.ProductID,
                    Product.ProductName,
                    Product.ProductNumber,
                    CostMeasure.UnitMeasureCode,
                    CostMeasure.Name AS CostMeasureName,
                    ProductVendor.AverageLeadTime,
                    ProductVendor.StandardPrice,
                    ProductReview.ReviewerName,
                    ProductReview.Rating,
                    Product.ProductCategory,
                    Product.ProductSubCategory
                FROM Product Product
                INNER JOIN Production.ProductModel
                ON ProductModel.ProductModelID = Product.ProductModelID
                LEFT JOIN Production.ProductReview
                ON ProductReview.ProductID = Product.ProductID
                LEFT JOIN Purchasing.ProductVendor
                ON ProductVendor.ProductID = Product.ProductID
                LEFT JOIN Production.UnitMeasure CostMeasure
                ON ProductVendor.UnitMeasureCode = CostMeasure.UnitMeasureCode;
                */
}