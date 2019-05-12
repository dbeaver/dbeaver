
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * List of all possible iterator or query specific warnings (e.g. hash spilling, no join predicate)
 * 
 * <p>Java class for WarningsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WarningsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="ColumnsWithNoStatistics" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="SpillToTempDb" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SpillToTempDbType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Wait" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}WaitWarningType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="PlanAffectingConvert" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}AffectingConvertWarningType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="SortSpillDetails" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SortSpillDetailsType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="HashSpillDetails" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}HashSpillDetailsType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="MemoryGrantWarning" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MemoryGrantWarningInfo" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attribute name="NoJoinPredicate" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="SpatialGuess" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="UnmatchedIndexes" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="FullUpdateForOnlineIndexBuild" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WarningsType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "columnsWithNoStatisticsOrSpillToTempDbOrWait"
})
public class WarningsType_sql2017 {

    @XmlElements({
        @XmlElement(name = "ColumnsWithNoStatistics", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = ColumnReferenceListType_sql2017 .class),
        @XmlElement(name = "SpillToTempDb", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = SpillToTempDbType_sql2017 .class),
        @XmlElement(name = "Wait", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = WaitWarningType_sql2017 .class),
        @XmlElement(name = "PlanAffectingConvert", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = AffectingConvertWarningType_sql2017 .class),
        @XmlElement(name = "SortSpillDetails", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = SortSpillDetailsType_sql2017 .class),
        @XmlElement(name = "HashSpillDetails", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = HashSpillDetailsType_sql2017 .class),
        @XmlElement(name = "MemoryGrantWarning", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = MemoryGrantWarningInfo_sql2017 .class)
    })
    protected List<Object> columnsWithNoStatisticsOrSpillToTempDbOrWait;
    @XmlAttribute(name = "NoJoinPredicate")
    protected Boolean noJoinPredicate;
    @XmlAttribute(name = "SpatialGuess")
    protected Boolean spatialGuess;
    @XmlAttribute(name = "UnmatchedIndexes")
    protected Boolean unmatchedIndexes;
    @XmlAttribute(name = "FullUpdateForOnlineIndexBuild")
    protected Boolean fullUpdateForOnlineIndexBuild;

    /**
     * Gets the value of the columnsWithNoStatisticsOrSpillToTempDbOrWait property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the columnsWithNoStatisticsOrSpillToTempDbOrWait property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getColumnsWithNoStatisticsOrSpillToTempDbOrWait().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ColumnReferenceListType_sql2017 }
     * {@link SpillToTempDbType_sql2017 }
     * {@link WaitWarningType_sql2017 }
     * {@link AffectingConvertWarningType_sql2017 }
     * {@link SortSpillDetailsType_sql2017 }
     * {@link HashSpillDetailsType_sql2017 }
     * {@link MemoryGrantWarningInfo_sql2017 }
     * 
     * 
     */
    public List<Object> getColumnsWithNoStatisticsOrSpillToTempDbOrWait() {
        if (columnsWithNoStatisticsOrSpillToTempDbOrWait == null) {
            columnsWithNoStatisticsOrSpillToTempDbOrWait = new ArrayList<Object>();
        }
        return this.columnsWithNoStatisticsOrSpillToTempDbOrWait;
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

    /**
     * Gets the value of the spatialGuess property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getSpatialGuess() {
        return spatialGuess;
    }

    /**
     * Sets the value of the spatialGuess property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSpatialGuess(Boolean value) {
        this.spatialGuess = value;
    }

    /**
     * Gets the value of the unmatchedIndexes property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getUnmatchedIndexes() {
        return unmatchedIndexes;
    }

    /**
     * Sets the value of the unmatchedIndexes property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUnmatchedIndexes(Boolean value) {
        this.unmatchedIndexes = value;
    }

    /**
     * Gets the value of the fullUpdateForOnlineIndexBuild property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getFullUpdateForOnlineIndexBuild() {
        return fullUpdateForOnlineIndexBuild;
    }

    /**
     * Sets the value of the fullUpdateForOnlineIndexBuild property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFullUpdateForOnlineIndexBuild(Boolean value) {
        this.fullUpdateForOnlineIndexBuild = value;
    }

}
