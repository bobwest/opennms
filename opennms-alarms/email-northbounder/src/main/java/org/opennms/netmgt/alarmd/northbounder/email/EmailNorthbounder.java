/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.alarmd.northbounder.email;

import java.util.List;
import java.util.Map;

import org.opennms.core.utils.PropertiesUtils;
import org.opennms.javamail.JavaMailerException;
import org.opennms.javamail.JavaSendMailer;
import org.opennms.netmgt.alarmd.api.NorthboundAlarm;
import org.opennms.netmgt.alarmd.api.NorthbounderException;
import org.opennms.netmgt.alarmd.api.support.AbstractNorthbounder;
import org.opennms.netmgt.config.javamail.SendmailConfig;
import org.opennms.netmgt.dao.api.JavaMailConfigurationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Forwards alarms via Email.
 * 
 * @author <a href="mailto:agalue@opennms.org>Alejandro Galue</a>
 */
public class EmailNorthbounder extends AbstractNorthbounder implements InitializingBean {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(EmailNorthbounder.class);

    /** The Constant NBI_NAME. */
    protected static final String NBI_NAME = "EmailNBI";

    /** The Email Configuration DAO. */
    private EmailNorthbounderConfigDao m_configDao;

    /** The Email Destination. */
    private EmailDestination m_destination;

    /** The Sendmail Configuration. */
    private SendmailConfig m_sendmail;

    /** The Email subject format. */
    private String m_emailSubjectFormat;

    /** The Email body format. */
    private String m_emailBodyFormat;

    /**
     * Instantiates a new SNMP Trap northbounder.
     *
     * @param configDao the SNMP Trap configuration DAO
     * @param javaMailDao the java mail DAO
     * @param destinationName the destination name
     */
    public EmailNorthbounder(EmailNorthbounderConfigDao configDao, JavaMailConfigurationDao javaMailDao, String destinationName) {
        super(NBI_NAME + ":" + destinationName);
        m_configDao = configDao;
        m_destination = configDao.getConfig().getEmailDestination(destinationName);
        m_sendmail = javaMailDao.getSendMailConfig(destinationName);
        if (m_sendmail != null && m_sendmail.getSendmailMessage() != null) {
            m_emailSubjectFormat = m_sendmail.getSendmailMessage().getSubject();
            m_emailBodyFormat = m_sendmail.getSendmailMessage().getBody();
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (m_destination == null || m_sendmail == null || m_emailSubjectFormat == null || m_emailBodyFormat == null) {
            LOG.info("Email Northbounder is currently disabled, rejecting alarm.");
            String msg = "Email forwarding configuration is not initialized.";
            IllegalStateException e = new IllegalStateException(msg);
            LOG.error(msg, e);
            throw e;
        }
        setNaglesDelay(getConfig().getNaglesDelay());
        setMaxBatchSize(getConfig().getBatchSize());
        setMaxPreservedAlarms(getConfig().getQueueSize());
    }

    /**
     * The abstraction makes a call here to determine if the alarm should be placed on the queue of alarms to be sent northerly.
     *
     * @param alarm the alarm
     * @return true, if successful
     */
    @Override
    public boolean accepts(NorthboundAlarm alarm) {
        if (!getConfig().isEnabled()) {
            return false;
        }
        LOG.debug("Validating UEI of alarm: {}", alarm.getUei());
        if (getConfig().getUeis() == null || getConfig().getUeis().contains(alarm.getUei())) {
            LOG.debug("UEI: {}, accepted.", alarm.getUei());
            boolean passed = m_destination.accepts(alarm);
            LOG.debug("Filters: {}, passed ? {}.", alarm.getUei(), passed);
            return passed;
        }
        LOG.debug("UEI: {}, rejected.", alarm.getUei());
        return false;
    }

    /**
     * Each implementation of the AbstractNorthbounder has a nice queue (Nagle's algorithmic) and the worker thread that processes the queue
     * calls this method to send alarms to the northern NMS.
     *
     * @param alarms the alarms
     * @throws NorthbounderException the northbounder exception
     */
    @Override
    public void forwardAlarms(List<NorthboundAlarm> alarms) throws NorthbounderException {
        if (alarms == null) {
            String errorMsg = "No alarms in alarms list for syslog forwarding.";
            NorthbounderException e = new NorthbounderException(errorMsg);
            LOG.error(errorMsg, e);
            throw e;
        }
        LOG.info("Forwarding {} alarms to destination {}", alarms.size(), m_destination.getName());
        for (NorthboundAlarm alarm : alarms) {
            try {
                JavaSendMailer mailer = new JavaSendMailer(getSendmailConfig(alarm), false);
                mailer.setDebug(true); // FIXME ?
                mailer.send();
            } catch (JavaMailerException e) {
                LOG.error("Can't send email for {}", alarm, e);
            }
        }
    }

    /**
     * Gets the sendmail configuration.
     *
     * @param alarm the northbound alarm
     * @return the sendmail configuration
     */
    protected SendmailConfig getSendmailConfig(NorthboundAlarm alarm) {
        Map<String, Object> mapping = createMapping(alarm);
        final String subject = PropertiesUtils.substitute(m_emailSubjectFormat, mapping);
        m_sendmail.getSendmailMessage().setSubject(subject);
        final String body = PropertiesUtils.substitute(m_emailBodyFormat, mapping);
        m_sendmail.getSendmailMessage().setBody(body);
        return m_sendmail;
    }

    /**
     * Gets the configuration.
     *
     * @return the configuration
     */
    protected EmailNorthbounderConfig getConfig() {
        return m_configDao.getConfig();
    }

}