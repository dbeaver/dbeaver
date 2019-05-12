
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * 
 * 				Information on single statistics used during query optimization.
 * 					Database : name of the database
 * 					Schema : name of the schema
 * 					Table : name of the table
 * 					Statistics : name of the statistics
 * 					ModificationCount : number of modifications since the last update
 * 					SamplingPercent : statistics sampling percentage
 * 					LastUpdate : date when the statistics was updated
 * 			
 * 
 * <p>Java class for StatsInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StatsInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="Database" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Schema" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Table" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Statistics" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ModificationCount" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="SamplingPercent" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="LastUpdate" type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatsInfoType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class StatsInfoType_sql2017 {

    @XmlAttribute(name = "Database")
    protected String database;
    @XmlAttribute(name = "Schema")
    protected String schema;
    @XmlAttribute(name = "Table")
    protected String table;
    @XmlAttribute(name = "Statistics", required = true)
    protected String statistics;
    @XmlAttribute(name = "ModificationCount", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger modificationCount;
    @XmlAttribute(name = "SamplingPercent", required = true)
    protected double samplingPercent;
    @XmlAttribute(name = "LastUpdate")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastUpdate;

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
     * Gets the value of the statistics property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatistics() {
        return statistics;
    }

    /**
     * Sets the value of the statistics property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatistics(String value) {
        this.statistics = value;
    }

    /**
     * Gets the value of the modificationCount property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getModificationCount() {
        return modificationCount;
    }

    /**
     * Sets the value of the modificationCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setModificationCount(BigInteger value) {
        this.modificationCount = value;
    }

    /**
     * Gets the value of the samplingPercent property.
     * 
     */
    public double getSamplingPercent() {
        return samplingPercent;
    }

    /**
     * Sets the value of the samplingPercent property.
     * 
     */
    public void setSamplingPercent(double value) {
        this.samplingPercent = value;
    }

    /**
     * Gets the value of the lastUpdate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Sets the value of the lastUpdate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setLastUpdate(XMLGregorianCalendar value) {
        this.lastUpdate = value;
    }

}
