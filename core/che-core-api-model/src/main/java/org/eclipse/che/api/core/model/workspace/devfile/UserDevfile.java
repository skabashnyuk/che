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
package org.eclipse.che.api.core.model.workspace.devfile;

/** Devfile that persisted in permanent storage. */
public interface UserDevfile {
  /** Returns the identifier of this persisted devfile instance. It is mandatory and unique. */
  String getId();
  /** Returns the name of devfile. It is mandatory. */
  String getName();

  /**
   * Returns the namespace of the current devfile instance. Devfile name is unique for devfiles in
   * the same namespace.
   */
  String getNamespace();

  /** Returns description of devfile */
  String getDescription();
  /** Returns devfile content */
  Devfile getDevfile();
}
