package modules

import com.google.inject.AbstractModule
import models.actor_managers.{DepartmentManager, EncryptionManager, UserAccountManager}
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindActor[EncryptionManager]("encryption-manager")
    bindActor[UserAccountManager]("user-account-manager")
    bindActor[DepartmentManager]("department-manager")
  }
}
