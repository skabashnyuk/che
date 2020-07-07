package org.eclipse.che.api.devfile.server.event;

import org.eclipse.che.api.core.model.workspace.devfile.UserDevfile;
import org.eclipse.che.api.core.notification.EventOrigin;
import org.eclipse.che.api.devfile.server.model.impl.UserDevfileImpl;
import org.eclipse.che.core.db.cascade.event.RemoveEvent;

/** Published before {@link UserDevfile user devfile} removed. */
@EventOrigin("user")
public class BeforeDevfileRemovedEvent extends RemoveEvent {

  private final UserDevfileImpl userDevfile;

  public BeforeDevfileRemovedEvent(UserDevfileImpl userDevfile) {
    this.userDevfile = userDevfile;
  }

  /** Returns user which is going to be removed. */
  public UserDevfileImpl getUserDevfile() {
    return userDevfile;
  }
}
