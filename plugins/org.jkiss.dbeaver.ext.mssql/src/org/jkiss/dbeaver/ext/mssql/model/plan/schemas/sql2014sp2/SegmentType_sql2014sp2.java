
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SegmentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SegmentType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="GroupBy" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType"/>
 *         &lt;element name="SegmentColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "groupBy",
    "segmentColumn",
    "relOp"
})
public class SegmentType_sql2014sp2
    extends RelOpBaseType_sql2014sp2
{

    @XmlElement(name = "GroupBy", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ColumnReferenceListType_sql2014sp2 groupBy;
    @XmlElement(name = "SegmentColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected SingleColumnReferenceType_sql2014sp2 segmentColumn;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2014sp2 relOp;

    /**
     * Gets the value of the groupBy property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2014sp2 }
     *     
     */
    public ColumnReferenceListType_sql2014sp2 getGroupBy() {
        return groupBy;
    }

    /**
     * Sets the value of the groupBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2014sp2 }
     *     
     */
    public void setGroupBy(ColumnReferenceListType_sql2014sp2 value) {
        this.groupBy = value;
    }

    /**
     * Gets the value of the segmentColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public SingleColumnReferenceType_sql2014sp2 getSegmentColumn() {
        return segmentColumn;
    }

    /**
     * Sets the value of the segmentColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public void setSegmentColumn(SingleColumnReferenceType_sql2014sp2 value) {
        this.segmentColumn = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2014sp2 }
     *     
     */
    public RelOpType_sql2014sp2 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2014sp2 }
     *     
     */
    public void setRelOp(RelOpType_sql2014sp2 value) {
        this.relOp = value;
    }

}
