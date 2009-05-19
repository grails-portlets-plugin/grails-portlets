package org.codehaus.grails.portlets.container;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

/**
 * @author Kenji Nakamura
 */
public abstract class AbstractPortletContainerAdapter
        implements PortletContainerAdapter,
        ApplicationContextAware {

    private static ApplicationContext ctx;

    /**
     * Instantiate an appropriate implementation based on the package name of the
     * parameter.
     *
     * @param obj Any portlet container specific implemntation. The package name is used.
     * @return an implemntation of this class suitable for the underlying portlet container.
     */
    public static PortletContainerAdapter getInstance(Object obj) {
        PortletContainerAdapter portletContainerAdapter = null;
        return (PortletContainerAdapter) ctx.getBean("portletContainerAdapter");
    }

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.ctx = applicationContext;
    }
}
