/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.devfile.server.model.impl;

import com.google.common.annotations.Beta;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.model.workspace.devfile.Devfile;
import org.eclipse.che.api.core.model.workspace.devfile.UserDevfile;
import org.eclipse.che.api.workspace.server.model.impl.devfile.DevfileImpl;

@Entity(name = "UserDevfile")
@Table(name = "userdevfile")
@NamedQueries({
  @NamedQuery(name = "UserDevfile.getAll", query = "SELECT d FROM UserDevfile d ORDER BY d.id"),
  @NamedQuery(name = "UserDevfile.getTotalCount", query = "SELECT COUNT(d) FROM UserDevfile d"),
})
@Beta
public class UserDevfileImpl implements UserDevfile {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "devfile_id")
  private DevfileImpl devfile;

  @Column(name = "generated_name")
  private String generateName;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @ManyToOne
  @JoinColumn(name = "accountid", nullable = false)
  private AccountImpl account;

  public UserDevfileImpl() {}

  public UserDevfileImpl(String id, Account account, UserDevfile userDevfile) {
    this(
        id, account, userDevfile.getName(), userDevfile.getDescription(), userDevfile.getDevfile());
  }

  public UserDevfileImpl(UserDevfile userDevfile, Account account) {
    this(userDevfile.getId(), account, userDevfile);
  }

  public UserDevfileImpl(UserDevfileImpl userDevfile) {
    this(
        userDevfile.id,
        userDevfile.account,
        userDevfile.getName(),
        userDevfile.getDescription(),
        userDevfile.getDevfile());
  }

  public UserDevfileImpl(
      String id, Account account, String name, String description, Devfile devfile) {
    this.id = id;
    this.account = new AccountImpl(account);
    this.name = name;
    this.description = description;
    this.devfile = new DevfileImpl(devfile);
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getNamespace() {
    return account.getName();
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public Devfile getDevfile() {
    return devfile;
  }

  public void setDevfile(DevfileImpl devfile) {
    this.devfile = devfile;
  }

  public AccountImpl getAccount() {
    return account;
  }

  public void setAccount(AccountImpl account) {
    this.account = account;
  }

  @PostLoad
  public void postLoad() {
    devfile.getMetadata().setGenerateName(generateName);
  }

  @PreUpdate
  @PrePersist
  public void beforeDb() {
    generateName = devfile.getMetadata().getGenerateName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserDevfileImpl that = (UserDevfileImpl) o;
    return Objects.equals(id, that.id)
        && devfile.equals(that.devfile)
        && name.equals(that.name)
        && Objects.equals(description, that.description)
        && account.equals(that.account);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, devfile, name, description, account);
  }

  @Override
  public String toString() {
    return "UserDevfileImpl{"
        + "id='"
        + id
        + '\''
        + ", devfile="
        + devfile
        + ", name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", account="
        + account
        + '}';
  }
}
