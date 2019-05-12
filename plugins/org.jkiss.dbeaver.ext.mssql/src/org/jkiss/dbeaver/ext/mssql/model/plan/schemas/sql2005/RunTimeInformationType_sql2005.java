
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Runtime information provided from statistics_xml for each relational iterator
 * 
 * <p>Java class for RunTimeInformationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RunTimeInformationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RunTimeCountersPerThread" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                 &lt;/sequence>
 *                 &lt;attribute name="Thread" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                 &lt;attribute name="ActualRebinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualRewinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualRows" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualEndOfScans" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualExecutions" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RunTimeInformationType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "runTimeCountersPerThread"
})
public class RunTimeInformationType_sql2005 {

    @XmlElement(name = "RunTimeCountersPerThread", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005> runTimeCountersPerThread;

    /**
     * Gets the value of the runTimeCountersPerThread property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the runTimeCountersPerThread property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRunTimeCountersPerThread().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005 }
     * 
     * 
     */
    public List<RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005> getRunTimeCountersPerThread() {
        if (runTimeCountersPerThread == null) {
            runTimeCountersPerThread = new ArrayList<RunTimeInformationType_sql2005 .RunTimeCountersPerThread_sql2005>();
        }
        return this.runTimeCountersPerThread;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *       &lt;/sequence>
     *       &lt;attribute name="Thread" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="ActualRebinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualRewinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualRows" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualEndOfScans" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualExecutions" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class RunTimeCountersPerThread_sql2005 {

        @XmlAttribute(name = "Thread", required = true)
        protected int thread;
        @XmlAttribute(name = "ActualRebinds")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRebinds;
        @XmlAttribute(name = "ActualRewinds")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRewinds;
        @XmlAttribute(name = "ActualRows", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRows;
        @XmlAttribute(name = "ActualEndOfScans", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualEndOfScans;
        @XmlAttribute(name = "ActualExecutions", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualExecutions;

        /**
         * Gets the value of the thread property.
         * 
         */
        public int getThread() {
            return thread;
        }

        /**
         * Sets the value of the thread property.
         * 
         */
        public void setThread(int value) {
            this.thread = value;
        }

        /**
         * Gets the value of the actualRebinds property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualRebinds() {
            return actualRebinds;
        }

        /**
         * Sets the value of the actualRebinds property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualRebinds(BigInteger value) {
            this.actualRebinds = value;
        }

        /**
         * Gets the value of the actualRewinds property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualRewinds() {
            return actualRewinds;
        }

        /**
         * Sets the value of the actualRewinds property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualRewinds(BigInteger value) {
            this.actualRewinds = value;
        }

        /**
         * Gets the value of the actualRows property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualRows() {
            return actualRows;
        }

        /**
         * Sets the value of the actualRows property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualRows(BigInteger value) {
            this.actualRows = value;
        }

        /**
         * Gets the value of the actualEndOfScans property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualEndOfScans() {
            return actualEndOfScans;
        }

        /**
         * Sets the value of the actualEndOfScans property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualEndOfScans(BigInteger value) {
            this.actualEndOfScans = value;
        }

        /**
         * Gets the value of the actualExecutions property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualExecutions() {
            return actualExecutions;
        }

        /**
         * Sets the value of the actualExecutions property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualExecutions(BigInteger value) {
            this.actualExecutions = value;
        }

    }

}
