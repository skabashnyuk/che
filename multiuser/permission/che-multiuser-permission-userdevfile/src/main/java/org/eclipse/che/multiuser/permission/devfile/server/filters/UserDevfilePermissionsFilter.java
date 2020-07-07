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
package org.eclipse.che.multiuser.permission.devfile.server.filters;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.devfile.server.UserDevfileManager;
import org.eclipse.che.api.devfile.server.UserDevfileService;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;

/**
 * Restricts access to methods of {@link UserDevfileService} by users' permissions.
 *
 * <p>Filter contains rules for protecting of all methods of {@link UserDevfileService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/userdevfile{path:(/.*)?}")
public class UserDevfilePermissionsFilter extends CheMethodInvokerFilter {
  private final UserDevfileManager userDevfileManager;

  @Inject
  public UserDevfilePermissionsFilter(UserDevfileManager userDevfileManager) {
    this.userDevfileManager = userDevfileManager;
  }

  @Override
  public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments)
      throws ForbiddenException, ServerException, NotFoundException {
    final String methodName = genericResourceMethod.getMethod().getName();

    final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
    String action;
    String key;

    switch (methodName) {
      case "create":
      case "getById":
      case "getUserDevfiles":
      case "update":
      case "delete":
      default:
        throw new ForbiddenException("The user does not have permission to perform this operation");
    }

  }
}
