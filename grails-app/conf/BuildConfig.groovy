grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {

		provided 'javax.portlet:portlet-api:2.0'

		runtime('org.springframework:spring-webmvc-portlet:3.0.5.RELEASE') {
			excludes 'spring-beans'
			excludes 'spring-context'
			excludes 'spring-core'
			excludes 'spring-web'
			excludes 'spring-webmvc'
		}

		compile 'org.aspectj:aspectjtools:1.6.10'
	}

	plugins {
		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}
	}
}
