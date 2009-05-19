package org.codehaus.grails.portlets;

import org.springframework.web.portlet.DispatcherPortlet;
import org.springframework.web.portlet.context.PortletApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.util.Assert;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext;
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils;

import javax.portlet.*;
import javax.servlet.ServletContext;

import grails.util.GrailsUtil;

/**
 * @author Lee Butts
 */
public class GrailsDispatcherPortlet extends DispatcherPortlet {
    public static final String PORTLET_NAME = "grails.portlet.name";
    public static final String PORTLET_CONFIG = "grails.portlet.config";

    private GrailsApplication application;

    protected void doActionService(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
        addPortletInfoToRequest(actionRequest);
        super.doActionService(actionRequest, actionResponse);
    }

    protected void doRenderService(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
        addPortletInfoToRequest(renderRequest);
        super.doRenderService(renderRequest, renderResponse);
    }

    private void addPortletInfoToRequest(PortletRequest portletRequest) {
        addPortletNameToRequest(portletRequest);
        addPortletConfigToRequest(portletRequest);
    }

    private void addPortletConfigToRequest(PortletRequest portletRequest) {
        portletRequest.setAttribute(PORTLET_CONFIG, getPortletConfig());
    }

    private void addPortletNameToRequest(PortletRequest request) {
        String portletName = getPortletConfig().getPortletName();
        request.setAttribute(PORTLET_NAME, portletName);
    }
}
