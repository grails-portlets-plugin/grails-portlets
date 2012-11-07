import java.lang.reflect.Modifier

import org.codehaus.grails.portlets.GrailsPortletClass
import org.codehaus.grails.portlets.GrailsPortletHandlerAdapter
import org.codehaus.grails.portlets.GrailsPortletHandlerInterceptor
import org.codehaus.grails.portlets.GrailsPortletHandlerMapping
import org.codehaus.grails.portlets.PortletArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder as RCH

class PortletsGrailsPlugin {

    def version = "0.9.2"
    def grailsVersion = "2.0 > *"
    def loadAfter = ['controllers']
    def artefacts = [PortletArtefactHandler]

    def author = "Kenji Nakamura"
    def authorEmail = "kenji_nakamura@diva-america.com"
    def title = "Portlets Plugin"
    def description = 'Generate JSR-168 compliant portlet war.'
    def documentation = "http://grails.org/plugins/portlets"

    String license = 'MIT'
    def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPPORTLETS']
    def scm = [url: 'https://github.com/grails-portlets-plugin/grails-portlets']
    def developers = [[name: 'Philip Wu', email: 'wu.phil@gmail.com']]

    def watchedResources = [
        'file:./grails-app/portlets/**/*Portlet.groovy',
        'file:./plugins/*/grails-app/portlets/**/*Portlet.groovy'
    ]

    static final String WEB_APP_NAME = 'Grails Portlet Application'

    def doWithSpring = {

        application.portletClasses.each { portlet ->
            log.debug "Configuring portlet $portlet.fullName"

            "${portlet.fullName}Class"(MethodInvokingFactoryBean) {
                targetObject = ref("grailsApplication", true)
                targetMethod = "getArtefact"
                arguments = [PortletArtefactHandler.TYPE, portlet.fullName]
            }
            "${portlet.fullName}TargetSource"(HotSwappableTargetSource, ref("${portlet.fullName}Class"))

            "${portlet.fullName}Proxy"(ProxyFactoryBean) {
                targetSource = ref("${portlet.fullName}TargetSource")
                proxyInterfaces = [GrailsPortletClass]
            }
            "${portlet.fullName}"("${portlet.fullName}Proxy": "newInstance") {bean ->
                bean.singleton = false
                bean.autowire = "byName"
            }
        }

        portletHandlerMappings(GrailsPortletHandlerMapping) {
            interceptors = [ref("portletHandlerInterceptor")]
        }

        portletHandlerAdapter(GrailsPortletHandlerAdapter)

        portletHandlerInterceptor(GrailsPortletHandlerInterceptor)
    }

    def doWithWebDescriptor = {webXml ->
        def mappingElement = webXml.'servlet-mapping'
        mappingElement = mappingElement[mappingElement.size() - 1]

        mappingElement + {
            'servlet-mapping' {
                'servlet-name'('view-servlet')
                'url-pattern'('/WEB-INF/servlet/view')
            }
        }

        def servletElement = webXml.'servlet'
        servletElement = servletElement[servletElement.size() - 1]

        servletElement + {
            'servlet' {
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
            //def controllersPlugin = new ControllersGrailsPlugin()
            //controllersPlugin.registerControllerMethods(mc, ctx)

            def enhancer = new MetaClassEnhancer()
            enhancer.addApi(new ControllersApi(getManager()))
            enhancer.enhance mc

            Class superClass = portletClass.superclass

            mc.getPluginContextPath = { ->
                ctx.pluginMetaManager.getPluginPathForResource(delegate.getClass().name) ?: ''
            }

            mc.getPortletRequest = { -> getFromRequestAttributes('javax.portlet.request') }

            mc.getPortletResponse = { -> getFromRequestAttributes('javax.portlet.response') }

            mc.getMode = { -> getPortletRequest().portletMode }

            mc.getSession = { -> getPortletRequest().portletSession }

            mc.getWindowState = { -> getPortletRequest().windowState }

            mc.getPortalContext = { -> getPortletRequest().portalContext }

            mc.getPreferences = { -> getPortletRequest().preferences }

            // deal with abstract super classes
            while (superClass != Object) {
                if (Modifier.isAbstract(superClass.getModifiers())) {
                    WebMetaUtils.registerCommonWebProperties(superClass.metaClass, application)
                    //controllersPlugin.registerControllerMethods(superClass.metaClass, ctx)
                    enhancer = new MetaClassEnhancer()
                    enhancer.addApi(new ControllersApi(getManager()))
                    enhancer.enhance superClass.metaClass
                }
            }
        }
    }

    private getFromRequestAttributes(key) {
        RCH.currentRequestAttributes().getAttribute(key, RequestAttributes.SCOPE_REQUEST)
    }

    def onChange = { event ->
        def context = event.ctx
        if (!context) {
            log.debug("Application context not found. Can't reload")
            return
        }

        boolean isNew = application.getPortletClass(event.source?.name) ? false : true
        def portletClass = application.addArtefact(PortletArtefactHandler.TYPE, event.source)

        if (isNew) {
            log.info "Portlet ${event.source} found. You need to restart for the change to be applied"
        }
        else {
            log.debug("Portlet ${event.source} changed. Reloading...")

            context.getBean("${portletClass.fullName}TargetSource").swap(portletClass)
        }
        event.manager?.getGrailsPlugin("portlets")?.doWithDynamicMethods(event.ctx)
    }

    def generateTomcatContextFile() {

    }
}
