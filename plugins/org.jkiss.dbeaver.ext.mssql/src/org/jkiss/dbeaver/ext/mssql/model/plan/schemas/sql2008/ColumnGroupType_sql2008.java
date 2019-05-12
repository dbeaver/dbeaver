
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ColumnGroupType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ColumnGroupType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Column" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Usage" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="EQUALITY"/>
 *             &lt;enumeration value="INEQUALITY"/>
 *             &lt;enumeration value="INCLUDE"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ColumnGroupType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "column"
})
public class ColumnGroupType_sql2008 {

    @XmlElement(name = "Column", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<ColumnType_sql2008> column;
    @XmlAttribute(name = "Usage", required = true)
    protected String usage;

    /**
     * Gets the value of the column property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the column property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getColumn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ColumnType_sql2008 }
     * 
     * 
     */
    public List<ColumnType_sql2008> getColumn() {
        if (column == null) {
            column = new ArrayList<ColumnType_sql2008>();
        }
        return this.column;
    }

    /**
     * Gets the value of the usage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsage() {
        return usage;
    }

    /**
     * Sets the value of the usage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsage(String value) {
        this.usage = value;
    }

}
