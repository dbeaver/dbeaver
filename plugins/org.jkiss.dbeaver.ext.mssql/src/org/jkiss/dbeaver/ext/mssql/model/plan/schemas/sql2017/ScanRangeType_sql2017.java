
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ScanRangeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScanRangeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RangeColumns" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType"/>
 *         &lt;element name="RangeExpressions" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionListType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ScanType" use="required" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CompareOpType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScanRangeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "rangeColumns",
    "rangeExpressions"
})
public class ScanRangeType_sql2017 {

    @XmlElement(name = "RangeColumns", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ColumnReferenceListType_sql2017 rangeColumns;
    @XmlElement(name = "RangeExpressions", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ScalarExpressionListType_sql2017 rangeExpressions;
    @XmlAttribute(name = "ScanType", required = true)
    protected CompareOpType_sql2017 scanType;

    /**
     * Gets the value of the rangeColumns property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public ColumnReferenceListType_sql2017 getRangeColumns() {
        return rangeColumns;
    }

    /**
     * Sets the value of the rangeColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public void setRangeColumns(ColumnReferenceListType_sql2017 value) {
        this.rangeColumns = value;
    }

    /**
     * Gets the value of the rangeExpressions property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionListType_sql2017 }
     *     
     */
    public ScalarExpressionListType_sql2017 getRangeExpressions() {
        return rangeExpressions;
    }

    /**
     * Sets the value of the rangeExpressions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionListType_sql2017 }
     *     
     */
    public void setRangeExpressions(ScalarExpressionListType_sql2017 value) {
        this.rangeExpressions = value;
    }

    /**
     * Gets the value of the scanType property.
     * 
     * @return
     *     possible object is
     *     {@link CompareOpType_sql2017 }
     *     
     */
    public CompareOpType_sql2017 getScanType() {
        return scanType;
    }

    /**
     * Sets the value of the scanType property.
     * 
     * @param value
     *     allowed object is
     *     {@link CompareOpType_sql2017 }
     *     
     */
    public void setScanType(CompareOpType_sql2017 value) {
        this.scanType = value;
    }

}
