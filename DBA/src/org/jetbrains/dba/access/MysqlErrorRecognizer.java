package org.jetbrains.dba.access;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.dba.errors.DBError;
import org.jetbrains.dba.errors.DuplicateKeyError;
import org.jetbrains.dba.errors.UnknownDBError;

import java.sql.SQLException;



/**
 * MySQL specific errors recognizer.
 **/
public class MysqlErrorRecognizer extends BaseErrorRecognizer {

  @NotNull
  @Override
  protected DBError recognizeSpecificError(@NotNull final SQLException sqlException) {
    int code = sqlException.getErrorCode();

    switch (code) {
      case 1062:
        return new DuplicateKeyError(sqlException);
      default:
        return new UnknownDBError(sqlException);
    }
  }
}
