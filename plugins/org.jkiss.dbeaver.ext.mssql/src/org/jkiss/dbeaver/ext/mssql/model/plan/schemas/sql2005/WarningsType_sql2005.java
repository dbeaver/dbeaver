
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * List of all possible iterator specific warnings (e.g. hash spilling, no join predicate
 * 
 * <p>Java class for WarningsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WarningsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ColumnsWithNoStatistics" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="NoJoinPredicate" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WarningsType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "columnsWithNoStatistics"
})
public class WarningsType_sql2005 {

    @XmlElement(name = "ColumnsWithNoStatistics", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2005 columnsWithNoStatistics;
    @XmlAttribute(name = "NoJoinPredicate")
    protected Boolean noJoinPredicate;

    /**
     * Gets the value of the columnsWithNoStatistics property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2005 }
     *     
     */
    public ColumnReferenceListType_sql2005 getColumnsWithNoStatistics() {
        return columnsWithNoStatistics;
    }

    /**
     * Sets the value of the columnsWithNoStatistics property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2005 }
     *     
     */
    public void setColumnsWithNoStatistics(ColumnReferenceListType_sql2005 value) {
        this.columnsWithNoStatistics = value;
    }

    /**
     * Gets the value of the noJoinPredicate property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getNoJoinPredicate() {
        return noJoinPredicate;
    }

    /**
     * Sets the value of the noJoinPredicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNoJoinPredicate(Boolean value) {
        this.noJoinPredicate = value;
    }

}
