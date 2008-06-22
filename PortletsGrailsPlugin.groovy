import org.springframework.web.context.request.RequestContextHolder as RCH

import grails.util.GrailsUtil
import org.codehaus.grails.portlets.*
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource

class PortletsGrailsPlugin {

    static WEB_APP_NAME = 'Grails Portlet Application'

    def watchedResources = ['file:./grails-app/controllers/**/*Portlet.groovy',
            'file:./plugins/*/grails-app/controllers/**/*Portlet.groovy'
            ]

    def version = 0.1
    def dependsOn = [controllers: '1.1-SNAPSHOT']
    def artefacts = [PortletArtefactHandler.class]

    def doWithSpring = {
        //def controllerPlugin = new ControllersGrailsPlugin()
        application.portletClasses.each {portlet ->
            log.debug "Configuring portlet $portlet.fullName"
            /*def controllerPluginClosure = controllerPlugin.configureAOPProxyBean
            controllerPluginClosure.delegate = delegate
            controllerPluginClosure.call(portlet, PrtletArtefactHandler.TYPE,
                    org.codehaus.grails.portlets.GrailsPortletClass.class, false)*/
            "${portlet.fullName}Class"(MethodInvokingFactoryBean) {
                targetObject = ref("grailsApplication", true)
                targetMethod = "getArtefact"
                arguments = [PortletArtefactHandler.TYPE, portlet.fullName]
            }
            "${portlet.fullName}TargetSource"(HotSwappableTargetSource, ref("${portlet.fullName}Class"))

            "${portlet.fullName}Proxy"(ProxyFactoryBean) {
                targetSource = ref("${portlet.fullName}TargetSource")
                proxyInterfaces = [GrailsPortletClass.class]
            }
            "${portlet.shortName}"("${portlet.fullName}Proxy": "newInstance") {bean ->
                bean.singleton = false
                bean.autowire = "byName"
            }
        }
        portletHandlerMappings(GrailsPortletHandlerMapping) {
            interceptors = [ref("portletHandlerInterceptor")]
        }
        portletHandlerAdapter(GrailsPortletHandlerAdapter)
        portletReloadFilter(PortletReloadFilter)
        portletHandlerInterceptor(GrailsPortletHandlerInterceptor){
            portletReloadFilter = ref(portletReloadFilter)
        }
    }

    def doWithWebDescriptor = {webXml ->
        def mappingElement = webXml.'servlet-mapping'
        mappingElement = mappingElement[mappingElement.size()-1]

        mappingElement + {
            'servlet-mapping' {
                'servlet-name'('view-servlet')
                'url-pattern'('/WEB-INF/servlet/view')
            }
        }

        def servletElement = webXml.'servlet'
        servletElement = servletElement[servletElement.size()-1]

        servletElement + {
            'servlet' {
                'servlet-name'('view-servlet')
                'servlet-class'('org.springframework.web.servlet.ViewRendererServlet')
                'load-on-startup'('1')
            }
        }

        if (GrailsUtil.isDevelopmentEnv() && watchedResources.length > 0) {
            log.info("Creating Pluto servlets for ${watchedResources.length} portlets...")
            for (Resource portlet in watchedResources) {
                def portletName = portlet.filename - 'Portlet.groovy'
                servletElement + {
                    'servlet' {
                        'servlet-name'(portletName)
                        'servlet-class'('org.apache.pluto.core.PortletServlet')
                        'init-param' {
                            'param-name'('portlet-name')
                            'param-value'(portletName)
                        }
                        'load-on-startup'('1')
                    }
                }
                mappingElement + {
                    'servlet-mapping' {
                        'servlet-name'(portletName)
                        'url-pattern'("/PlutoInvoker/${portletName}")
                    }
                }
            }
        }
    }

    def doWithDynamicMethods = {ApplicationContext ctx ->
        def registry = GroovySystem.getMetaClassRegistry()

        def bind = new BindDynamicMethod()

        // add commons objects and dynamic methods like render and redirect to portlets
        for (GrailsClass portlet in application.portletClasses) {
            MetaClass mc = portlet.metaClass
            Class portletClass = portlet.clazz
            WebMetaUtils.registerCommonWebProperties(mc, application)
            def controllersPlugin = new ControllersGrailsPlugin()
            controllersPlugin.registerControllerMethods(mc, ctx)
            Class superClass = portletClass.superclass

            mc.getPluginContextPath = {->
                PluginMetaManager metaManager = ctx.pluginMetaManager
                String path = metaManager.getPluginPathForResource(delegate.class.name)
                path ? path : ''
            }

            mc.getMode = {->
                def webRequest = RCH.currentRequestAttributes();
                webRequest.getCurrentRequest().getPortletMode()
            }

            mc.getSession = {->
                def webRequest = RCH.currentRequestAttributes();
                webRequest.getCurrentRequest().getPortletSession(true)
            }

            mc.getWindowState = {->
                def webRequest = RCH.currentRequestAttributes();
                webRequest.getCurrentRequest().getWindowState()
            }

            mc.getPortletConfig = {->
                def webRequest = RCH.currentRequestAttributes();
                webRequest.getCurrentRequest().getAttribute(GrailsDispatcherPortlet.PORTLET_CONFIG);
            }

            mc.getPortalContext = {->
                def webRequest = RCH.currentRequestAttributes();
                webRequest.getCurrentRequest().getPortalContext()
            }

            mc.getPreferences = { ->
                def webRequest = RCH.currentRequestAttributes();
                webRequest.getCurrentRequest().getPreferences()
            }

            // deal with abstract super classes
            while (superClass != Object.class) {
                if (Modifier.isAbstract(superClass.getModifiers())) {
                    WebMetaUtils.registerCommonWebProperties(superClass.metaClass, application)
                    controllersPlugin.registerControllerMethods(superClass.metaClass, ctx)
                }
            }
        }
    }

    def onChange = {event ->
        def context = event.ctx
        if (!context) {
            if (log.isDebugEnabled())
                log.debug("Application context not found. Can't reload")
            return
        }
        boolean isNew = application.getPortletClass(event.source?.name) ? false : true
        def portletClass = application.addArtefact(PortletArtefactHandler.TYPE, event.source)

        if (isNew) {
            //TODO dynamic adding of portlets to pluto when in DEV
            log.info "Portlet ${event.source} found. You need to restart for the change to be applied"
        }
        else {
            if (log.isDebugEnabled())
                log.debug("Portlet ${event.source} changed. Reloading...")

            def portletTargetSource = context.getBean("${portletClass.fullName}TargetSource")
            portletTargetSource.swap(portletClass)
        }
        event.manager?.getGrailsPlugin("portlets")?.doWithDynamicMethods(event.ctx)
    }

}