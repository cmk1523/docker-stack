import hudson.security.FullControlOnceLoggedInAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import jenkins.install.InstallState
import jenkins.model.Jenkins

def instance = Jenkins.get()
def username = System.getenv("JENKINS_ADMIN_ID")
def password = System.getenv("JENKINS_ADMIN_PASSWORD")

if (!username?.trim() || !password?.trim()) {
    throw new IllegalStateException("JENKINS_ADMIN_ID and JENKINS_ADMIN_PASSWORD must be set")
}

def realm = instance.securityRealm
def needsAccount = !(realm instanceof HudsonPrivateSecurityRealm) || realm.getAllUsers().isEmpty()

if (needsAccount) {
    def localRealm = new HudsonPrivateSecurityRealm(false)
    localRealm.createAccount(username, password)
    instance.securityRealm = localRealm

    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.setAllowAnonymousRead(false)
    instance.authorizationStrategy = strategy
}

if (instance.installState != InstallState.INITIAL_SETUP_COMPLETED) {
    instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
}

instance.save()
