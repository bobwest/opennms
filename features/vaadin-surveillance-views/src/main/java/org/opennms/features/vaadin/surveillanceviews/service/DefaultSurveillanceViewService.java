package org.opennms.features.vaadin.surveillanceviews.service;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.criteria.Alias;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.criteria.Order;
import org.opennms.core.criteria.restrictions.Restrictions;
import org.opennms.features.vaadin.surveillanceviews.model.Category;
import org.opennms.features.vaadin.surveillanceviews.model.View;
import org.opennms.netmgt.config.CategoryFactory;
import org.opennms.netmgt.config.GroupDao;
import org.opennms.netmgt.config.categories.CatFactory;
import org.opennms.netmgt.config.categories.Categorygroup;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.dao.api.AlarmRepository;
import org.opennms.netmgt.dao.api.CategoryDao;
import org.opennms.netmgt.dao.api.GraphDao;
import org.opennms.netmgt.dao.api.MonitoredServiceDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.api.NotificationDao;
import org.opennms.netmgt.dao.api.OutageDao;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.dao.api.SurveillanceViewConfigDao;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsOutage;
import org.opennms.netmgt.model.SurveillanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class that encapsulate helper methods for surveillance views.
 */
public class DefaultSurveillanceViewService implements SurveillanceViewService {

    /**
     * The logger to be used
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSurveillanceViewService.class);

    /**
     * DAO instances injected via blueprint.xml
     */
    private NodeDao m_nodeDao;
    private ResourceDao m_resourceDao;
    private GraphDao m_graphDao;
    private NotificationDao m_notificationDao;
    private SurveillanceViewConfigDao m_surveillanceViewConfigDao;
    private CategoryDao m_categoryDao;
    private AlarmDao m_alarmDao;
    private GroupDao m_groupDao;
    private OutageDao m_outageDao;
    private AlarmRepository m_alarmRepository;
    private MonitoredServiceDao m_monitoredServiceDao;
    private TransactionOperations m_transactionOperations;

    /**
     * Method to set the alarm repository instance.
     *
     * @param alarmRepository the repository to be used
     */
    public void setAlarmRepository(AlarmRepository alarmRepository) {
        this.m_alarmRepository = alarmRepository;
    }

    /**
     * Method to set the node dao.
     *
     * @param nodeDao the {@link org.opennms.netmgt.dao.api.NodeDao} to be used
     */
    public void setNodeDao(NodeDao nodeDao) {
        this.m_nodeDao = nodeDao;
    }

    /**
     * Method to set the monitored service dao.
     *
     * @param monitoredServiceDao the {@link org.opennms.netmgt.dao.api.MonitoredServiceDao} to be used
     */
    public void setMonitoredServiceDao(MonitoredServiceDao monitoredServiceDao) {
        this.m_monitoredServiceDao = monitoredServiceDao;
    }

    /**
     * Method to set the resource dao.
     *
     * @param resourceDao the {@link org.opennms.netmgt.dao.api.ResourceDao} to be used
     */
    public void setResourceDao(ResourceDao resourceDao) {
        this.m_resourceDao = resourceDao;
    }

    /**
     * Method to set the graph dao.
     *
     * @param graphDao the {@link org.opennms.netmgt.dao.api.GraphDao} to be used
     */
    public void setGraphDao(GraphDao graphDao) {
        this.m_graphDao = graphDao;
    }

    /**
     * Method to set the notification dao.
     *
     * @param notificationDao the {@link org.opennms.netmgt.dao.api.NotificationDao} to be used
     */
    public void setNotificationDao(NotificationDao notificationDao) {
        this.m_notificationDao = notificationDao;
    }

    /**
     * Method to set the surveillance view config  dao.
     *
     * @param surveillanceViewConfigDao the {@link org.opennms.netmgt.dao.api.SurveillanceViewConfigDao} to be used
     */
    public void setSurveillanceViewConfigDao(SurveillanceViewConfigDao surveillanceViewConfigDao) {
        this.m_surveillanceViewConfigDao = surveillanceViewConfigDao;
    }

    /**
     * Method to set the category dao.
     *
     * @param categoryDao the {@link org.opennms.netmgt.dao.api.CategoryDao} to be used
     */
    public void setCategoryDao(CategoryDao categoryDao) {
        this.m_categoryDao = categoryDao;
    }

    /**
     * Method to set the alarm dao.
     *
     * @param alarmDao the {@link org.opennms.netmgt.dao.api.AlarmDao} to be used
     */
    public void setAlarmDao(AlarmDao alarmDao) {
        this.m_alarmDao = alarmDao;
    }

    /**
     * Method to set the group dao.
     *
     * @param groupDao the {@link org.opennms.netmgt.config.GroupDao} to be used
     */
    public void setGroupDao(GroupDao groupDao) {
        this.m_groupDao = groupDao;
    }

    /**
     * Method to set the outage dao.
     *
     * @param outageDao the {@link org.opennms.netmgt.dao.api.OutageDao} to be used
     */
    public void setOutageDao(OutageDao outageDao) {
        this.m_outageDao = outageDao;
    }

    /**
     * Method to set the transaction operations instance
     *
     * @param transactionOperations
     */
    public void setTransactionOperations(TransactionOperations transactionOperations) {
        this.m_transactionOperations = transactionOperations;
    }

    /**
     * Retrieves a list of OpenNMS categories from the DAO instance.
     *
     * @return the list of categories
     */
    @Override
    public List<OnmsCategory> getOnmsCategories() {
        return m_transactionOperations.execute(new TransactionCallback<List<OnmsCategory>>() {
            @Override
            public List<OnmsCategory> doInTransaction(TransactionStatus transactionStatus) {
                return m_categoryDao.findAll();
            }
        });
    }

    /**
     * Returns a list of Rtc catgories.
     *
     * @return the list of Rtc categories.
     */
    public List<String> getRtcCategories() {

        CatFactory cFactory = null;

        try {
            CategoryFactory.init();
            cFactory = CategoryFactory.getInstance();

        } catch (IOException ex) {
            LOG.error("Failed to load categories information", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (MarshalException ex) {
            LOG.error("Failed to load categories information", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            LOG.error("Failed to load categories information", ex);
            throw new UndeclaredThrowableException(ex);
        }

        List<String> categories = new ArrayList<String>();

        cFactory.getReadLock().lock();

        try {
            for (Categorygroup cg : cFactory.getConfig().getCategorygroupCollection()) {
                for (final org.opennms.netmgt.config.categories.Category category : cg.getCategories().getCategoryCollection()) {
                    categories.add(category.getLabel());
                }
            }
        } finally {
            cFactory.getReadLock().unlock();
        }

        return categories;
    }

    public AlarmDao getAlarmDao() {
        return m_alarmDao;
    }

    public AlarmRepository getAlarmRepository() {
        return m_alarmRepository;
    }

    public NodeDao getNodeDao() {
        return m_nodeDao;
    }

    @Override
    public NotificationDao getNotificationDao() {
        return m_notificationDao;
    }

    public Set<OnmsCategory> getOnmsCategoriesFromViewCategories(final Collection<Category> viewCats) {
        final Set<OnmsCategory> categories = new HashSet<OnmsCategory>();

        for (final Category viewCat : viewCats) {
            final OnmsCategory category = m_categoryDao.findByName(viewCat.getName());

            if (category == null) {
                throw new ObjectRetrievalFailureException(OnmsCategory.class, viewCat.getName(), "Unable to locate OnmsCategory named: " + viewCat.getName() + " as specified in the surveillance view configuration file", null);
            }
            categories.add(category);
        }
        return categories;
    }


    public SurveillanceStatus[][] calculateCellStatus(final View view) {
        return m_transactionOperations.execute(new TransactionCallback<SurveillanceStatus[][]>() {
            @Override
            public SurveillanceStatus[][] doInTransaction(TransactionStatus transactionStatus) {

                final SurveillanceStatus[][] cellStatus = new SurveillanceStatus[view.getRows().size()][view.getColumns().size()];
                for (int rowIndex = 0; rowIndex < view.getRows().size(); rowIndex++) {
                    for (int colIndex = 0; colIndex < view.getColumns().size(); colIndex++) {
                        final Collection<OnmsCategory> rowCategories = getOnmsCategoriesFromViewCategories(view.getRows().get(rowIndex).getCategories());
                        final Collection<OnmsCategory> columnCategories = getOnmsCategoriesFromViewCategories(view.getColumns().get(colIndex).getCategories());
                        final SurveillanceStatus status = m_nodeDao.findSurveillanceStatusByCategoryLists(rowCategories, columnCategories);

                        cellStatus[rowIndex][colIndex] = status;
                    }
                }
                return cellStatus;
            }
        });
    }

    private List<NodeRtc> getNodeListForCriteria(Criteria serviceCriteria, Criteria outageCriteria) {
        List<Order> ordersService = new ArrayList<>();
        ordersService.add(Order.asc("node.label"));
        ordersService.add(Order.asc("node.id"));
        ordersService.add(Order.asc("ipInterface.ipAddress"));
        ordersService.add(Order.asc("serviceType.name"));
        serviceCriteria.setOrders(ordersService);

        Date periodEnd = new Date(System.currentTimeMillis());
        Date periodStart = new Date(periodEnd.getTime() - (24 * 60 * 60 * 1000));

        outageCriteria.addRestriction(Restrictions.any(Restrictions.isNull("ifRegainedService"), Restrictions.ge("ifLostService", periodStart), Restrictions.ge("ifRegainedService", periodStart)));

        List<Order> ordersOutage = new ArrayList<>();
        ordersOutage.add(Order.asc("monitoredService"));
        ordersOutage.add(Order.asc("ifLostService"));
        outageCriteria.setOrders(ordersOutage);

        List<OnmsMonitoredService> services = m_monitoredServiceDao.findMatching(serviceCriteria);
        List<OnmsOutage> outages = m_outageDao.findMatching(outageCriteria);

        Map<OnmsMonitoredService, Long> serviceDownTime = calculateServiceDownTime(periodEnd, periodStart, outages);

        List<NodeRtc> model = new ArrayList<>();

        OnmsNode lastNode = null;
        int serviceCount = 0;
        int serviceDownCount = 0;
        long downMillisCount = 0;
        for (OnmsMonitoredService service : services) {
            if (!service.getIpInterface().getNode().equals(lastNode) && lastNode != null) {
                Double availability = calculateAvailability(serviceCount, downMillisCount);

                model.add(new NodeRtc(lastNode, serviceCount, serviceDownCount, availability));

                serviceCount = 0;
                serviceDownCount = 0;
                downMillisCount = 0;
            }

            serviceCount++;
            if (service.isDown()) {
                serviceDownCount++;
            }

            Long downMillis = serviceDownTime.get(service);
            if (downMillis != null) {
                downMillisCount += downMillis;
            }

            lastNode = service.getIpInterface().getNode();
        }
        if (lastNode != null) {
            Double availability = calculateAvailability(serviceCount, downMillisCount);

            model.add(new NodeRtc(lastNode, serviceCount, serviceDownCount, availability));
        }

        return model;
    }

    private Map<OnmsMonitoredService, Long> calculateServiceDownTime(Date periodEnd, Date periodStart, List<OnmsOutage> outages) {
        Map<OnmsMonitoredService, Long> map = new HashMap<OnmsMonitoredService, Long>();
        for (OnmsOutage outage : outages) {
            if (map.get(outage.getMonitoredService()) == null) {
                map.put(outage.getMonitoredService(), Long.valueOf(0));
            }

            Date begin;
            if (outage.getIfLostService().before(periodStart)) {
                begin = periodStart;
            } else {
                begin = outage.getIfLostService();
            }

            Date end;
            if (outage.getIfRegainedService() == null || !outage.getIfRegainedService().before(periodEnd)) {
                end = periodEnd;
            } else {
                end = outage.getIfRegainedService();
            }

            Long count = map.get(outage.getMonitoredService());
            count += (end.getTime() - begin.getTime());
            map.put(outage.getMonitoredService(), count);
        }
        return map;
    }

    private Double calculateAvailability(int serviceCount, long downMillisCount) {
        long upMillis = ((long) serviceCount * (24L * 60L * 60L * 1000L)) - downMillisCount;

        return ((double) upMillis / (double) (serviceCount * (24 * 60 * 60 * 1000)));
    }

    public List<NodeRtc> getRtcList(List<OnmsNode> nodes) {
        CriteriaBuilder outageCb = new CriteriaBuilder(OnmsOutage.class);

        outageCb.alias("monitoredService", "monitoredService", Alias.JoinType.INNER_JOIN);
        outageCb.alias("monitoredService.ipInterface", "ipInterface", Alias.JoinType.INNER_JOIN);
        outageCb.alias("monitoredService.ipInterface.node", "node", Alias.JoinType.INNER_JOIN);
        outageCb.eq("monitoredService.status", "A");
        outageCb.ne("ipInterface.isManaged", "D");
        outageCb.ne("node.type", "D");

        /*

        System.out.println("--- IN ---");

        for (OnmsNode onmsNode : nodes) {
            System.out.println(onmsNode.getLabel());
        }

        System.out.println(outageCb.toCriteria());

        */

        CriteriaBuilder serviceCb = new CriteriaBuilder(OnmsMonitoredService.class);

        serviceCb.alias("ipInterface", "ipInterface", Alias.JoinType.INNER_JOIN);
        serviceCb.alias("ipInterface.node", "node", Alias.JoinType.INNER_JOIN);
        serviceCb.alias("serviceType", "serviceType", Alias.JoinType.INNER_JOIN);
        serviceCb.alias("currentOutages", "currentOutages", Alias.JoinType.INNER_JOIN);
        // serviceCb.eq("monitoredService.status", "A");
        serviceCb.eq("status", "A");
        serviceCb.ne("ipInterface.isManaged", "D");
        serviceCb.ne("node.type", "D");

        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        serviceCb.in("ipInterface.node", nodes);

        return getNodeListForCriteria(serviceCb.toCriteria(), outageCb.toCriteria());
    }

    public TransactionOperations getTransactionOperations() {
        return m_transactionOperations;
    }
}
