
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

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
 *                 &lt;attribute name="BrickId" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                 &lt;attribute name="ActualRebinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualRewinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualRows" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualRowsRead" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="Batches" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualEndOfScans" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualExecutions" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualExecutionMode" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ExecutionModeType" />
 *                 &lt;attribute name="TaskAddr" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="SchedulerId" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="FirstActiveTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="LastActiveTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="OpenTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="FirstRowTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="LastRowTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="CloseTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualElapsedms" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualCPUms" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualScans" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualLogicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualPhysicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualReadAheads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualLobLogicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualLobPhysicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="ActualLobReadAheads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="SegmentReads" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                 &lt;attribute name="SegmentSkips" type="{http://www.w3.org/2001/XMLSchema}int" />
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
public class RunTimeInformationType_sql2014sp2 {

    @XmlElement(name = "RunTimeCountersPerThread", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2> runTimeCountersPerThread;

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
     * {@link RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2 }
     * 
     * 
     */
    public List<RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2> getRunTimeCountersPerThread() {
        if (runTimeCountersPerThread == null) {
            runTimeCountersPerThread = new ArrayList<RunTimeInformationType_sql2014sp2 .RunTimeCountersPerThread_sql2014sp2>();
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
     *       &lt;attribute name="BrickId" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="ActualRebinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualRewinds" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualRows" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualRowsRead" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="Batches" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualEndOfScans" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualExecutions" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualExecutionMode" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ExecutionModeType" />
     *       &lt;attribute name="TaskAddr" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="SchedulerId" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="FirstActiveTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="LastActiveTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="OpenTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="FirstRowTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="LastRowTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="CloseTime" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualElapsedms" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualCPUms" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualScans" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualLogicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualPhysicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualReadAheads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualLobLogicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualLobPhysicalReads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="ActualLobReadAheads" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="SegmentReads" type="{http://www.w3.org/2001/XMLSchema}int" />
     *       &lt;attribute name="SegmentSkips" type="{http://www.w3.org/2001/XMLSchema}int" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class RunTimeCountersPerThread_sql2014sp2 {

        @XmlAttribute(name = "Thread", required = true)
        protected int thread;
        @XmlAttribute(name = "BrickId")
        protected Integer brickId;
        @XmlAttribute(name = "ActualRebinds")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRebinds;
        @XmlAttribute(name = "ActualRewinds")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRewinds;
        @XmlAttribute(name = "ActualRows", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRows;
        @XmlAttribute(name = "ActualRowsRead")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualRowsRead;
        @XmlAttribute(name = "Batches")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger batches;
        @XmlAttribute(name = "ActualEndOfScans", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualEndOfScans;
        @XmlAttribute(name = "ActualExecutions", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualExecutions;
        @XmlAttribute(name = "ActualExecutionMode")
        protected ExecutionModeType_sql2014sp2 actualExecutionMode;
        @XmlAttribute(name = "TaskAddr")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger taskAddr;
        @XmlAttribute(name = "SchedulerId")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger schedulerId;
        @XmlAttribute(name = "FirstActiveTime")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger firstActiveTime;
        @XmlAttribute(name = "LastActiveTime")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger lastActiveTime;
        @XmlAttribute(name = "OpenTime")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger openTime;
        @XmlAttribute(name = "FirstRowTime")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger firstRowTime;
        @XmlAttribute(name = "LastRowTime")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger lastRowTime;
        @XmlAttribute(name = "CloseTime")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger closeTime;
        @XmlAttribute(name = "ActualElapsedms")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualElapsedms;
        @XmlAttribute(name = "ActualCPUms")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualCPUms;
        @XmlAttribute(name = "ActualScans")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualScans;
        @XmlAttribute(name = "ActualLogicalReads")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualLogicalReads;
        @XmlAttribute(name = "ActualPhysicalReads")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualPhysicalReads;
        @XmlAttribute(name = "ActualReadAheads")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualReadAheads;
        @XmlAttribute(name = "ActualLobLogicalReads")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualLobLogicalReads;
        @XmlAttribute(name = "ActualLobPhysicalReads")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualLobPhysicalReads;
        @XmlAttribute(name = "ActualLobReadAheads")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger actualLobReadAheads;
        @XmlAttribute(name = "SegmentReads")
        protected Integer segmentReads;
        @XmlAttribute(name = "SegmentSkips")
        protected Integer segmentSkips;

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
         * Gets the value of the brickId property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getBrickId() {
            return brickId;
        }

        /**
         * Sets the value of the brickId property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setBrickId(Integer value) {
            this.brickId = value;
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
         * Gets the value of the actualRowsRead property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualRowsRead() {
            return actualRowsRead;
        }

        /**
         * Sets the value of the actualRowsRead property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualRowsRead(BigInteger value) {
            this.actualRowsRead = value;
        }

        /**
         * Gets the value of the batches property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getBatches() {
            return batches;
        }

        /**
         * Sets the value of the batches property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setBatches(BigInteger value) {
            this.batches = value;
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

        /**
         * Gets the value of the actualExecutionMode property.
         * 
         * @return
         *     possible object is
         *     {@link ExecutionModeType_sql2014sp2 }
         *     
         */
        public ExecutionModeType_sql2014sp2 getActualExecutionMode() {
            return actualExecutionMode;
        }

        /**
         * Sets the value of the actualExecutionMode property.
         * 
         * @param value
         *     allowed object is
         *     {@link ExecutionModeType_sql2014sp2 }
         *     
         */
        public void setActualExecutionMode(ExecutionModeType_sql2014sp2 value) {
            this.actualExecutionMode = value;
        }

        /**
         * Gets the value of the taskAddr property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getTaskAddr() {
            return taskAddr;
        }

        /**
         * Sets the value of the taskAddr property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setTaskAddr(BigInteger value) {
            this.taskAddr = value;
        }

        /**
         * Gets the value of the schedulerId property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getSchedulerId() {
            return schedulerId;
        }

        /**
         * Sets the value of the schedulerId property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setSchedulerId(BigInteger value) {
            this.schedulerId = value;
        }

        /**
         * Gets the value of the firstActiveTime property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getFirstActiveTime() {
            return firstActiveTime;
        }

        /**
         * Sets the value of the firstActiveTime property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setFirstActiveTime(BigInteger value) {
            this.firstActiveTime = value;
        }

        /**
         * Gets the value of the lastActiveTime property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getLastActiveTime() {
            return lastActiveTime;
        }

        /**
         * Sets the value of the lastActiveTime property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setLastActiveTime(BigInteger value) {
            this.lastActiveTime = value;
        }

        /**
         * Gets the value of the openTime property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getOpenTime() {
            return openTime;
        }

        /**
         * Sets the value of the openTime property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setOpenTime(BigInteger value) {
            this.openTime = value;
        }

        /**
         * Gets the value of the firstRowTime property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getFirstRowTime() {
            return firstRowTime;
        }

        /**
         * Sets the value of the firstRowTime property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setFirstRowTime(BigInteger value) {
            this.firstRowTime = value;
        }

        /**
         * Gets the value of the lastRowTime property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getLastRowTime() {
            return lastRowTime;
        }

        /**
         * Sets the value of the lastRowTime property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setLastRowTime(BigInteger value) {
            this.lastRowTime = value;
        }

        /**
         * Gets the value of the closeTime property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getCloseTime() {
            return closeTime;
        }

        /**
         * Sets the value of the closeTime property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setCloseTime(BigInteger value) {
            this.closeTime = value;
        }

        /**
         * Gets the value of the actualElapsedms property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualElapsedms() {
            return actualElapsedms;
        }

        /**
         * Sets the value of the actualElapsedms property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualElapsedms(BigInteger value) {
            this.actualElapsedms = value;
        }

        /**
         * Gets the value of the actualCPUms property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualCPUms() {
            return actualCPUms;
        }

        /**
         * Sets the value of the actualCPUms property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualCPUms(BigInteger value) {
            this.actualCPUms = value;
        }

        /**
         * Gets the value of the actualScans property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualScans() {
            return actualScans;
        }

        /**
         * Sets the value of the actualScans property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualScans(BigInteger value) {
            this.actualScans = value;
        }

        /**
         * Gets the value of the actualLogicalReads property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualLogicalReads() {
            return actualLogicalReads;
        }

        /**
         * Sets the value of the actualLogicalReads property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualLogicalReads(BigInteger value) {
            this.actualLogicalReads = value;
        }

        /**
         * Gets the value of the actualPhysicalReads property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualPhysicalReads() {
            return actualPhysicalReads;
        }

        /**
         * Sets the value of the actualPhysicalReads property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualPhysicalReads(BigInteger value) {
            this.actualPhysicalReads = value;
        }

        /**
         * Gets the value of the actualReadAheads property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualReadAheads() {
            return actualReadAheads;
        }

        /**
         * Sets the value of the actualReadAheads property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualReadAheads(BigInteger value) {
            this.actualReadAheads = value;
        }

        /**
         * Gets the value of the actualLobLogicalReads property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualLobLogicalReads() {
            return actualLobLogicalReads;
        }

        /**
         * Sets the value of the actualLobLogicalReads property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualLobLogicalReads(BigInteger value) {
            this.actualLobLogicalReads = value;
        }

        /**
         * Gets the value of the actualLobPhysicalReads property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualLobPhysicalReads() {
            return actualLobPhysicalReads;
        }

        /**
         * Sets the value of the actualLobPhysicalReads property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualLobPhysicalReads(BigInteger value) {
            this.actualLobPhysicalReads = value;
        }

        /**
         * Gets the value of the actualLobReadAheads property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getActualLobReadAheads() {
            return actualLobReadAheads;
        }

        /**
         * Sets the value of the actualLobReadAheads property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setActualLobReadAheads(BigInteger value) {
            this.actualLobReadAheads = value;
        }

        /**
         * Gets the value of the segmentReads property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getSegmentReads() {
            return segmentReads;
        }

        /**
         * Sets the value of the segmentReads property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setSegmentReads(Integer value) {
            this.segmentReads = value;
        }

        /**
         * Gets the value of the segmentSkips property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public Integer getSegmentSkips() {
            return segmentSkips;
        }

        /**
         * Sets the value of the segmentSkips property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setSegmentSkips(Integer value) {
            this.segmentSkips = value;
        }

    }

}
