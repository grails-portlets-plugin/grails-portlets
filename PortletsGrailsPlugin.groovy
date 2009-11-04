import org.springframework.web.context.request.RequestContextHolder as RCH
import org.springframework.web.context.request.RequestAttributes 

import grails.util.GrailsUtil
import javax.portlet.PortletRequest
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

    // the plugin version
    def version = "0.7"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"
    // the other plugins this plugin depends on
    def loadAfter = ['controllers']

    def artefacts = [PortletArtefactHandler.class]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Kenji Nakamura"
    def authorEmail = "kenji_nakamura@diva-america.com"
    def title = "Portlets Plugin"
    def description = '''\\
Generate JSR-168 compliant portlet war. 
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugins/portlets"

    static WEB_APP_NAME = 'Grails Portlet Application'

    def watchedResources = ['file:./grails-app/portlets/**/*Portlet.groovy',
            'file:./plugins/*/grails-app/portlets/**/*Portlet.groovy'
    ]


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
        portletHandlerInterceptor(GrailsPortletHandlerInterceptor) {
            portletReloadFilter = ref(portletReloadFilter)
        }
    }

    def doWithWebDescriptor = {webXml ->
        def mappingElement = webXml.'servlet-mapping'
        mappingElement = mappingElement[mappingElement.size() - 1]

        mappingElement + {
            'servlet-mapping'
            {
                'servlet-name'('view-servlet')
                'url-pattern'('/WEB-INF/servlet/view')
            }
        }

        def servletElement = webXml.'servlet'
        servletElement = servletElement[servletElement.size() - 1]

        servletElement + {
            'servlet'
            {
                'servlet-name'('view-servlet')
                'servlet-class'('org.springframework.web.servlet.ViewRendererServlet')
                'load-on-startup'('1')
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

            mc.getPortletRequest = { 
                getFromRequestAttributes('javax.portlet.request')
            }

            mc.getPortletResponse = {
                getFromRequestAttributes('javax.portlet.response')
            }

            mc.getMode = {
                def req = getFromRequestAttributes('javax.portlet.request')
                req.portletMode
            }

            mc.getSession = {
                def req = getFromRequestAttributes('javax.portlet.request')
                req.portletSession
            }

            mc.getWindowState = {
                def req = getFromRequestAttributes('javax.portlet.request')
                req.windowState
            }

            mc.getPortalContext = {
                def req = getFromRequestAttributes('javax.portlet.request')
                req.portalContext
            }

            mc.getPreferences = {
                def req = getFromRequestAttributes('javax.portlet.request')
                req.preferences
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

    private getFromRequestAttributes(key) {
        def webRequest = RCH.currentRequestAttributes();
        webRequest.getAttribute(key,
                        RequestAttributes.SCOPE_REQUEST)
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

    def generateTomcatContextFile() {

    }

}
