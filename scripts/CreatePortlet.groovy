/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Creates a new Grails portlet.
 * 
 * @author Kenji Nakamura
 */
includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsBootstrap")

target(createPortlet: "Creates a new portlet") {
	depends(checkVersion, parseArguments)

	String type = "Portlet"
	promptForName(type: type)

	String name = argsMap.params[0]
	createArtifact(name: name, suffix: type, type: type, path: "grails-app/portlets")
	createUnitTest(name: name, suffix: type, superClass: "ControllerUnitTestCase")

	String viewsDir = "${basedir}/grails-app/views/${propertyName}"
	ant.mkdir(dir:viewsDir)

	event("CreatedFile", [viewsDir])
}

setDefaultTarget(createPortlet)
