package org.eclipse.che.multiuser.permission.devfile.server.model.impl;

import org.eclipse.che.api.devfile.server.model.impl.UserDevfileImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.multiuser.api.permission.server.model.impl.AbstractPermissions;
import org.eclipse.che.multiuser.permission.devfile.server.UserDevfileDomain;
import org.eclipse.che.multiuser.permission.devfile.server.model.UserDevfilePermissions;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Data object for {@link UserDevfilePermissions}
 *
 * @author Sergii Leschenko
 */
@Entity(name = "UserDevfilePermissions")
@NamedQueries({
  @NamedQuery(
      name = "UserDevfilePermissions.getByUserDevfileId",
      query =
          "SELECT permission "
              + "FROM UserDevfilePermissions permission "
              + "WHERE permission.userDevfileId = :userDevfileId "),
  @NamedQuery(
      name = "UserDevfilePermissions.getCountByUserDevfileId",
      query =
          "SELECT COUNT(permission) "
              + "FROM UserDevfilePermissions permission "
              + "WHERE permission.userDevfileId = :userDevfileId "),
  @NamedQuery(
      name = "UserDevfilePermissions.getByUserId",
      query =
          "SELECT permission "
              + "FROM  UserDevfilePermissions permission "
              + "WHERE permission.userId = :userId "),
  @NamedQuery(
      name = "UserDevfilePermissions.getByUserAndUserDevfileId",
      query =
          "SELECT permission  "
              + "FROM UserDevfilePermissions permission "
              + "WHERE permission.userId = :userId "
              + "AND permission.userDevfileId = :userDevfileId ",
      hints = {@QueryHint(name = "eclipselink.query-results-cache", value = "true")})
})
@Table(name = "che_userdevfile_permissions")
public class UserDevfilePermissionsImpl extends AbstractPermissions
    implements UserDevfilePermissions {

  @Column(name = "userdevfile_id")
  private String userDevfileId;

  @ManyToOne
  @JoinColumn(name = "userdevfile_id", insertable = false, updatable = false)
  private UserDevfileImpl userDevfile;

  @ElementCollection(fetch = FetchType.EAGER)
  @Column(name = "actions")
  @CollectionTable(name = "che_userdevfile_permissions_actions", joinColumns = @JoinColumn(name = "userdevfilepermissions_id"))
  protected List<String> actions;


  public UserDevfilePermissionsImpl() {}

  public UserDevfilePermissionsImpl(String userDevfileId, String userId, List<String> actions) {
    super(userId);
    this.userDevfileId = userDevfileId;
    if (actions != null) {
      this.actions = new ArrayList<>(actions);
    }
  }

  public UserDevfilePermissionsImpl(UserDevfilePermissions userDevfilePermissions) {
    this(userDevfilePermissions.getUserDevfileId(), userDevfilePermissions.getUserId(), userDevfilePermissions.getActions());
  }

  @Override
  public String getInstanceId() {
    return userDevfileId;
  }

  @Override
  public String getDomainId() {
    return UserDevfileDomain.DOMAIN_ID;
  }

  @Override
  public List<String> getActions() {
    return actions;
  }

  @Override
  public String getUserDevfileId() {
    return userDevfileId;
  }


  @Override
  public String toString() {
    return "UserDevfilePermissionsImpl{" +
            "userDevfileId='" + userDevfileId + '\'' +
            ", userDevfile=" + userDevfile +
            ", actions=" + actions +
            "} " + super.toString();
  }
}
