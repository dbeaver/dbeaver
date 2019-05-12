
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				List of statistics info used during query optimization
 * 			
 * 
 * <p>Java class for OptimizerStatsUsageType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OptimizerStatsUsageType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="StatisticsInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StatsInfoType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OptimizerStatsUsageType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "statisticsInfo"
})
public class OptimizerStatsUsageType_sql2017 {

    @XmlElement(name = "StatisticsInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<StatsInfoType_sql2017> statisticsInfo;

    /**
     * Gets the value of the statisticsInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the statisticsInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStatisticsInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StatsInfoType_sql2017 }
     * 
     * 
     */
    public List<StatsInfoType_sql2017> getStatisticsInfo() {
        if (statisticsInfo == null) {
            statisticsInfo = new ArrayList<StatsInfoType_sql2017>();
        }
        return this.statisticsInfo;
    }

}
