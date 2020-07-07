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
package org.eclipse.che.multiuser.permission.devfile.server;

import com.google.common.collect.ImmutableList;
import org.eclipse.che.multiuser.api.permission.server.AbstractPermissionsDomain;
import org.eclipse.che.multiuser.permission.devfile.server.model.impl.UserDevfilePermissionsImpl;

import java.util.List;

/**
 * Domain for storing workspaces' permissions
 *
 * @author Sergii Leschenko
 */
public class UserDevfileDomain extends AbstractPermissionsDomain<UserDevfilePermissionsImpl> {
  public static final String READ = "read";
  public static final String DELETE = "delete";

  public static final String DOMAIN_ID = "userDevfile";

  public UserDevfileDomain() {
    super(DOMAIN_ID, ImmutableList.of(READ, DELETE));
  }

  @Override
  public UserDevfilePermissionsImpl doCreateInstance(
      String userId, String instanceId, List<String> allowedActions) {
    return new UserDevfilePermissionsImpl(instanceId, userId, allowedActions);
  }
}
