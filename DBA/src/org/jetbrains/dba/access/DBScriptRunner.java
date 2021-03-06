package org.jetbrains.dba.access;

/**
 * @author Leonid Bushuev from JetBrains
 */
public interface DBScriptRunner {

  /**
   * Performs the script - all commands one by one.
   */
  DBScriptRunner run();
}

