import hudson.model.*
import jenkins.model.*

Thread.start {
  sleep 10000

  println '--> disable security'

  System.setProperty('hudson.model.DirectoryBrowserSupport.CSP', '')
  System.setProperty('permissive-script-security.enabled', 'true')
  println '--> disable security like csp... done'

  System.setProperty('hudson.security.csrf.GlobalCrumbIssuerConfiguration.DISABLE_CSRF_PROTECTION', 'true')
  System.setProperty('hudson.security.csrf.DefaultCrumbIssuer.EXCLUDE_SESSION_ID', 'true')
  println '--> disable security like crumb... done'
}
