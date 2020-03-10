#!/usr/bin/groovy
package com.test.jenkins

import jenkins.model.Jenkins
import com.cloudbees.groovy.cps.NonCPS

/**
 * Various utility methods (including HTTP connections).
 *
 * @see <a href="//https://github.com/jenkinsci/pipeline-examples/blob/master/docs/BEST_PRACTICES.md">
 *     Jenkins Pipeline BEST PRACTICES
 *     </a>
 */
class Utils {
    @NonCPS static def entries(def m) {m.collect {k, v -> [k, v]}}

    /**
     * Helper method. Necessary, because Jenkins does not allow to iterate over Map or use collect method in CPS context.
     *
     * @param map
     * @return a list of two-item lists
     */
    @NonCPS static List<List<?>> mapToList(Map map) {
        return map.collect { it ->
            [it.key, it.value]
        }
    }

    @NonCPS static def nodeRootDir(def nodeName) {
        for (node in Jenkins.instance.nodes) {
            if (!nodeName.equals('master') && node.name.compareTo(nodeName) == 0 ) {
                return node.getRootPath()
            }
        }
    }

    @NonCPS static def stashRestCall(def apiUrl, def login, def password, def method, def query=""){
        def token = ("${login}:${password}").bytes.encodeBase64().toString()
        def headers = [:]
        headers["Content-Type"] = "application/json"
        headers["X-Atlassian-Token"] = "no-check"
        headers["Authorization"] = "Basic ${token}"
	    
        return Utils.callURL("${apiUrl}", "${query}", method, headers)
    }

    @NonCPS static def callURL(def urlString) {
        Utils.callURL(urlString, "GET")
    }

    @NonCPS static def callURL(def urlString, def method) {
        Utils.callURL(urlString, "", method)
    }

    @NonCPS static def jsonCallURL(def urlString, def query, def method, def headers=null) {
        headers = headers ?: [:]
        headers["Accept"] = "application/json"
        if (method == 'POST' || method == 'DELETE') {
            headers["Content-Type"] = "application/json"
            query = query ? groovy.json.JsonOutput.toJson(query) : null
        }
        def response = Utils.callURL(urlString, query, method, headers)
        return new groovy.json.JsonSlurper().parseText(response)
    }

    @NonCPS static def callURL(def urlString, def queryString, def method, def headers=null) {
        if (queryString) {
            if (method == 'GET') {
                urlString += (urlString.contains('?') ? '&' : '?') + queryString
            }
        }

        def url = new URL(urlString)
        def connection = url.openConnection()
        connection.setRequestMethod(method)
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (headers) {
            for (p in headers) {
                connection.setRequestProperty(p.key, p.value)
            }
        }
        if (queryString) {
            if (method == 'POST' || method == 'DELETE') {
                connection.doOutput = true

                def writer = new OutputStreamWriter(connection.outputStream)
                writer.write(queryString)
                writer.flush()
                writer.close()
            }
        }

        connection.connect()

        def inputStream  = null
        if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            inputStream = connection.getInputStream()
        } else {
            inputStream = connection.getErrorStream()
        }

        def responseString = new StringBuffer()
        def reader = new BufferedReader(new InputStreamReader(inputStream))
        def line = ""
        while ((line = reader.readLine()) != null) {
          responseString.append(line)
        }
        reader.close()

        if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            return responseString.toString()
        } else {
            throw new Exception(
                    "${urlString} ${method} call returned" \
                    + " (${connection.responseCode})" \
                    + " ${responseString.toString()}")
        }
    }

}
