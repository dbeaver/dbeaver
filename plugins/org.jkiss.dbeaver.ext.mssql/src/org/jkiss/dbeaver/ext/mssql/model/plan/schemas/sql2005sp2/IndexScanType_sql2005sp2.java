
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IndexScanType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IndexScanType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RowsetType">
 *       &lt;sequence>
 *         &lt;element name="SeekPredicates" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicatesType" minOccurs="0"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="PartitionId" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="IndexedViewInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}IndexedViewInfoType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Lookup" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="Ordered" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="ScanDirection" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}OrderType" />
 *       &lt;attribute name="ForcedIndex" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="NoExpandHint" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IndexScanType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekPredicates",
    "predicate",
    "partitionId",
    "indexedViewInfo"
})
public class IndexScanType_sql2005sp2
    extends RowsetType_sql2005sp2
{

    @XmlElement(name = "SeekPredicates", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicatesType_sql2005sp2 seekPredicates;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2005sp2 predicate;
    @XmlElement(name = "PartitionId", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2005sp2 partitionId;
    @XmlElement(name = "IndexedViewInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected IndexedViewInfoType_sql2005sp2 indexedViewInfo;
    @XmlAttribute(name = "Lookup")
    protected Boolean lookup;
    @XmlAttribute(name = "Ordered", required = true)
    protected boolean ordered;
    @XmlAttribute(name = "ScanDirection")
    protected OrderType_sql2005sp2 scanDirection;
    @XmlAttribute(name = "ForcedIndex")
    protected Boolean forcedIndex;
    @XmlAttribute(name = "NoExpandHint")
    protected Boolean noExpandHint;

    /**
     * Gets the value of the seekPredicates property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicatesType_sql2005sp2 }
     *     
     */
    public SeekPredicatesType_sql2005sp2 getSeekPredicates() {
        return seekPredicates;
    }

    /**
     * Sets the value of the seekPredicates property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicatesType_sql2005sp2 }
     *     
     */
    public void setSeekPredicates(SeekPredicatesType_sql2005sp2 value) {
        this.seekPredicates = value;
    }

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2005sp2 }
     *     
     */
    public ScalarExpressionType_sql2005sp2 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2005sp2 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2005sp2 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the partitionId property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2005sp2 }
     *     
     */
    public SingleColumnReferenceType_sql2005sp2 getPartitionId() {
        return partitionId;
    }

    /**
     * Sets the value of the partitionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2005sp2 }
     *     
     */
    public void setPartitionId(SingleColumnReferenceType_sql2005sp2 value) {
        this.partitionId = value;
    }

    /**
     * Gets the value of the indexedViewInfo property.
     * 
     * @return
     *     possible object is
     *     {@link IndexedViewInfoType_sql2005sp2 }
     *     
     */
    public IndexedViewInfoType_sql2005sp2 getIndexedViewInfo() {
        return indexedViewInfo;
    }

    /**
     * Sets the value of the indexedViewInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexedViewInfoType_sql2005sp2 }
     *     
     */
    public void setIndexedViewInfo(IndexedViewInfoType_sql2005sp2 value) {
        this.indexedViewInfo = value;
    }

    /**
     * Gets the value of the lookup property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getLookup() {
        return lookup;
    }

    /**
     * Sets the value of the lookup property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLookup(Boolean value) {
        this.lookup = value;
    }

    /**
     * Gets the value of the ordered property.
     * 
     */
    public boolean isOrdered() {
        return ordered;
    }

    /**
     * Sets the value of the ordered property.
     * 
     */
    public void setOrdered(boolean value) {
        this.ordered = value;
    }

    /**
     * Gets the value of the scanDirection property.
     * 
     * @return
     *     possible object is
     *     {@link OrderType_sql2005sp2 }
     *     
     */
    public OrderType_sql2005sp2 getScanDirection() {
        return scanDirection;
    }

    /**
     * Sets the value of the scanDirection property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrderType_sql2005sp2 }
     *     
     */
    public void setScanDirection(OrderType_sql2005sp2 value) {
        this.scanDirection = value;
    }

    /**
     * Gets the value of the forcedIndex property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getForcedIndex() {
        return forcedIndex;
    }

    /**
     * Sets the value of the forcedIndex property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setForcedIndex(Boolean value) {
        this.forcedIndex = value;
    }

    /**
     * Gets the value of the noExpandHint property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getNoExpandHint() {
        return noExpandHint;
    }

    /**
     * Sets the value of the noExpandHint property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNoExpandHint(Boolean value) {
        this.noExpandHint = value;
    }

}
