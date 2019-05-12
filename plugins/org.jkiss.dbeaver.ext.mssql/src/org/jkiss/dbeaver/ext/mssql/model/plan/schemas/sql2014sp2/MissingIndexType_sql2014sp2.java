
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MissingIndexType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MissingIndexType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ColumnGroup" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnGroupType" maxOccurs="3"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Database" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Schema" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Table" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MissingIndexType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "columnGroup"
})
public class MissingIndexType_sql2014sp2 {

    @XmlElement(name = "ColumnGroup", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<ColumnGroupType_sql2014sp2> columnGroup;
    @XmlAttribute(name = "Database", required = true)
    protected String database;
    @XmlAttribute(name = "Schema", required = true)
    protected String schema;
    @XmlAttribute(name = "Table", required = true)
    protected String table;

    /**
     * Gets the value of the columnGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the columnGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getColumnGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ColumnGroupType_sql2014sp2 }
     * 
     * 
     */
    public List<ColumnGroupType_sql2014sp2> getColumnGroup() {
        if (columnGroup == null) {
            columnGroup = new ArrayList<ColumnGroupType_sql2014sp2>();
        }
        return this.columnGroup;
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

}
