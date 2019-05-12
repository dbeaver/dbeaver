
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ColumnReferenceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ColumnReferenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Server" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Database" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Schema" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Table" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Alias" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Column" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ComputedColumn" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="ParameterCompiledValue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ParameterRuntimeValue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ColumnReferenceType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "scalarOperator"
})
public class ColumnReferenceType_sql2005 {

    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarType_sql2005 scalarOperator;
    @XmlAttribute(name = "Server")
    protected String server;
    @XmlAttribute(name = "Database")
    protected String database;
    @XmlAttribute(name = "Schema")
    protected String schema;
    @XmlAttribute(name = "Table")
    protected String table;
    @XmlAttribute(name = "Alias")
    protected String alias;
    @XmlAttribute(name = "Column", required = true)
    protected String column;
    @XmlAttribute(name = "ComputedColumn")
    protected Boolean computedColumn;
    @XmlAttribute(name = "ParameterCompiledValue")
    protected String parameterCompiledValue;
    @XmlAttribute(name = "ParameterRuntimeValue")
    protected String parameterRuntimeValue;

    /**
     * Gets the value of the scalarOperator property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarType_sql2005 }
     *     
     */
    public ScalarType_sql2005 getScalarOperator() {
        return scalarOperator;
    }

    /**
     * Sets the value of the scalarOperator property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarType_sql2005 }
     *     
     */
    public void setScalarOperator(ScalarType_sql2005 value) {
        this.scalarOperator = value;
    }

    /**
     * Gets the value of the server property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServer() {
        return server;
    }

    /**
     * Sets the value of the server property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServer(String value) {
        this.server = value;
    }

    /**
     * Gets the value of the database property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the value of the database property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatabase(String value) {
        this.database = value;
    }

    /**
     * Gets the value of the schema property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the value of the schema property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSchema(String value) {
        this.schema = value;
    }

    /**
     * Gets the value of the table property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the value of the table property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTable(String value) {
        this.table = value;
    }

    /**
     * Gets the value of the alias property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Sets the value of the alias property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAlias(String value) {
        this.alias = value;
    }

    /**
     * Gets the value of the column property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getColumn() {
        return column;
    }

    /**
     * Sets the value of the column property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setColumn(String value) {
        this.column = value;
    }

    /**
     * Gets the value of the computedColumn property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getComputedColumn() {
        return computedColumn;
    }

    /**
     * Sets the value of the computedColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setComputedColumn(Boolean value) {
        this.computedColumn = value;
    }

    /**
     * Gets the value of the parameterCompiledValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParameterCompiledValue() {
        return parameterCompiledValue;
    }

    /**
     * Sets the value of the parameterCompiledValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParameterCompiledValue(String value) {
        this.parameterCompiledValue = value;
    }

    /**
     * Gets the value of the parameterRuntimeValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParameterRuntimeValue() {
        return parameterRuntimeValue;
    }

    /**
     * Sets the value of the parameterRuntimeValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParameterRuntimeValue(String value) {
        this.parameterRuntimeValue = value;
    }

}
