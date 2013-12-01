package org.codehaus.grails.portlets;

import static grails.util.GrailsNameUtils.getShortName;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.util.ConfigObject;

import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.WindowState;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * @author Lee Butts
 */
public class GrailsPortletHandlerAdapter implements HandlerAdapter, ApplicationContextAware, GrailsApplicationAware {

	private static final String ACTION_REQUEST_PARAM = "action";
	private static final String ACTION_CLOSURE_PREFIX = "action";
	private static final String RENDER_CLOSURE_PREFIX = "render";
	private static final String RESOURCE_CLOSURE_PREFIX = "resource";
	private static final String EVENT_CLOSURE_PREFIX = "event";

	private ApplicationContext applicationContext;
	private Log log = LogFactory.getLog(getClass());
	private GrailsApplication grailsApplication;

	public boolean supports(Object o) {
		return o instanceof GroovyObject;
	}

	public void handleAction(ActionRequest actionRequest, ActionResponse actionResponse, Object o) throws Exception {
		GroovyObject portlet = (GroovyObject) o;
		String action = actionRequest.getParameter(ACTION_REQUEST_PARAM);
		Closure<?> actionClosure = getPortletClosure(actionRequest, portlet, action, ACTION_CLOSURE_PREFIX);
		actionClosure.call();
	}

	public void handleEvent(EventRequest eventRequest,
	        EventResponse eventResponse, Object o) throws Exception {
		GroovyObject portlet = (GroovyObject) o;
		String action = eventRequest.getParameter(ACTION_REQUEST_PARAM);
		Closure<?> actionClosure = getPortletClosure(eventRequest, portlet, action, EVENT_CLOSURE_PREFIX);
		actionClosure.call();
	}

	public ModelAndView handleResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse, Object o) throws Exception {
		return handleRequest(resourceRequest, resourceResponse, o, RESOURCE_CLOSURE_PREFIX);
	}

	public ModelAndView handleRender(RenderRequest renderRequest, RenderResponse renderResponse, Object o) throws Exception {
		if (getMinimisedConfig() != null && renderRequest.getWindowState().equals(WindowState.MINIMIZED)) {
			log.info("portlet.handleMinimised is set, rendering empty string");
			renderResponse.setContentType("text/html");
			renderResponse.getPortletOutputStream().write("".getBytes());
			return null;
		}

		return handleRequest(renderRequest, renderResponse, o, RENDER_CLOSURE_PREFIX);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public boolean tryResolveView(String viewName) {
		ViewResolver vr = (ViewResolver) applicationContext.getBean("jspViewResolver");
		try {
			View view = vr.resolveViewName(viewName, LocaleContextHolder.getLocaleContext().getLocale());
			return view instanceof GroovyPageView; // GrailsViewResolver will return a GPV if it exists
			                                       // otherwise it's a normal JSP view (which may or may not exist)
		} catch (Exception e) {
			return false;
		}
	}

	private ModelAndView handleRequest(PortletRequest renderRequest, PortletResponse renderResponse, Object o, String closurePrefix) {
		GroovyObject portlet = (GroovyObject) o;
		String action = renderRequest.getParameter(ACTION_REQUEST_PARAM);
		Closure<?> render = getPortletClosure(renderRequest, portlet, action, closurePrefix);
		Object returnValue = render.call();
		if (returnValue instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> returnMap = (Map<String, Object>) returnValue;
			returnMap.put("portletRequest", renderRequest);
			returnMap.put("portletResponse", renderResponse);
			renderRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER, portlet);
			String viewName = "/" + getViewDir(renderRequest) + "/" + renderRequest.getParameter("action");
			if (tryResolveView(viewName)) {
				log.info("Trying to render action view " + viewName);
			} else {
				log.info("Couldn't resolve action view " + viewName);
				viewName = "/" + getViewDir(renderRequest) + "/" + renderRequest.getPortletMode().toString().toLowerCase();
				if (tryResolveView(viewName)) {
					log.info("Trying to render mode view " + viewName);
				} else {
					log.info("Couldn't resolve mode view " + viewName);
					viewName = "/" + getViewDir(renderRequest) + "/render";
					log.info("Trying to render view " + viewName);
				}
			}
			//FIX for GPPORTLETS-19
			return new ModelAndView(viewName, returnMap);
		}
		return null;
	}

	private Closure<?> getPortletClosure(PortletRequest request,
	        GroovyObject portlet, String actionParameter, String closurePrefix) {
		Closure<?> portletClosure = null;
		if (actionParameter != null) {
			try {
				portletClosure = (Closure<?>) portlet.getProperty(actionParameter);
			} catch (Exception e) {
				log.warn("Unable to find Closure property " + actionParameter + " from action request parameter");
			}
		}

		if (portletClosure == null) {
			String portletMode = request.getPortletMode().toString().toLowerCase();
			String modeActionName = closurePrefix + StringUtils.capitalize(portletMode);
			try {
				portletClosure = (Closure<?>) portlet.getProperty(modeActionName);
			} catch (Exception e) {
				log.trace("Didn't find portlet mode " + closurePrefix + " closure: " + modeActionName);
			}
		}
		if (portletClosure == null) {
			String defaultParam = "do" + StringUtils.capitalize(closurePrefix);
			log.info("Falling back to " + defaultParam + " closure");
			portletClosure = (Closure<?>) portlet.getProperty(defaultParam);
		}
		return portletClosure;
	}

	private String getViewDir(PortletRequest request) {
		String shortName = getShortName(((String) request.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME)));
		return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
	}

	private Object getMinimisedConfig() {
		try {
			// TODO allow overriding config setting per portlet
			ConfigObject configObject = (ConfigObject) grailsApplication.getConfig().get("portlet");
			Object value = null;
			if (configObject != null) {
				value = configObject.get("handleMinimised");
			}
			if (value != null) {
				return value;
			}

			log.info("portlet.handleMinimised not set, proceeding with normal render");
			return null;
		} catch (ClassCastException e) {
			log.warn("Unable to determine portlet.handleMinimised setting");
			return null;
		}
	}

	public void setGrailsApplication(GrailsApplication application) {
		grailsApplication = application;
	}
}
