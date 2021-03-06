package org.jetbrains.dba.access;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.dba.errors.DBError;
import org.jetbrains.dba.errors.UnhandledTypeError;
import org.jetbrains.dba.sql.SQLCommand;
import org.jetbrains.dba.sql.SQLQuery;
import org.jetbrains.dba.sql.SQLScript;

import java.sql.*;



/**
 * Database session.
 * Encapsulates connection and provides useful methods to work with database.
 */
abstract class BaseSession implements DBSession {

  //// STATE \\\\

  @NotNull
  private final BaseFacade facade;

  @NotNull
  private final Connection connection;

  private final boolean connectionOwner;


  //// INITIALIZATION \\\\


  public BaseSession(@NotNull BaseFacade facade, @NotNull final Connection connection, boolean ownConnection) {
    this.facade = facade;
    this.connection = connection;
    this.connectionOwner = ownConnection;
  }


  //// TRANSACTIONS \\\\


  protected void beginTransaction() {
    try {
      connection.setAutoCommit(false);
    }
    catch (SQLException e) {
      throw recognizeError(e);
    }
  }


  protected void commit() {
    try {
      connection.commit();
    }
    catch (SQLException e) {
      rollback();
      throw recognizeError(e);
    }

    try {
      connection.setAutoCommit(true);
    }
    catch (SQLException e) {
      // TODO
      e.printStackTrace();
    }
  }


  protected void rollback() {
    try {
      connection.rollback();
    }
    catch (SQLException rollbackException) {
      // TODO kernel panic!
      rollbackException.printStackTrace();
    }

    try {
      connection.setAutoCommit(true);
    }
    catch (SQLException e) {
      // TODO
      e.printStackTrace();
    }
  }


  //// CLOSURES \\\\


  @Override
  public <R> R inTransaction(final InTransaction<R> operation) {
    beginTransaction();
    try {
      final R result = operation.run(this);
      commit();
      return result;
    }
    finally {
      rollback();
    }
  }


  @Override
  public void inTransaction(final InTransactionNoResult operation) {
    beginTransaction();
    boolean committed = false;
    try {
      operation.run(this);
      commit();
      committed = true;
    }
    finally {
      if (!committed) rollback();
    }
  }


  //// USEFUL METHODS \\\\


  @Override
  public <S> DBQueryRunner<S> query(@NotNull final SQLQuery<S> query) {
    return new BaseQueryRunner<S>(this, query);
  }


  <S> S processQuery(@NotNull final String queryText,
                     @Nullable final Object[] params,
                     @NotNull final DBRowsCollector<S> collector) {
    try {
      boolean expectManyRows = collector.expectManyRows();
      PreparedStatement stmt = prepareStatementForQuery(queryText, expectManyRows);
      try {
        if (params != null) {
          assignParameters(stmt, params);
        }
        final ResultSet rset = stmt.executeQuery();
        try {
          return collector.collectRows(rset);
        }
        finally {
          rset.close();
        }
      }
      finally {
        stmt.close();
      }
    }
    catch (SQLException e) {
      throw recognizeError(e);
    }
  }


  @NotNull
  PreparedStatement prepareStatementForQuery(@NotNull final String queryText, boolean expectManyRows)
    throws SQLException {
    return connection.prepareStatement(queryText, ResultSet.TYPE_FORWARD_ONLY,
                                       ResultSet.CONCUR_READ_ONLY,
                                       ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }


  @Override
  public BaseCommandRunner command(@NotNull final SQLCommand command) {
    return new BaseCommandRunner(this, command.getSourceText());
  }


  @Override
  public BaseCommandRunner command(@NotNull final String commandText) {
    return new BaseCommandRunner(this, commandText);
  }


  @NotNull
  CallableStatement prepareCall(@NotNull final String statementText)
    throws SQLException
  {
    return connection.prepareCall(statementText);
  }


  protected void assignParameters(@NotNull final PreparedStatement stmt, final Object[] params)
    throws SQLException
  {
    if (params == null) {
      return;
    }
    for (int i = 0; i < params.length; i++) {
      Object param = params[i];
      assignParameter(stmt, i + 1, param);
    }
  }


  protected void assignParameter(@NotNull final PreparedStatement stmt,
                                 final int index,
                                 @Nullable final Object object)
    throws SQLException
  {
    if (object == null) {
      stmt.setNull(index, Types.BIT);
    }
    else if (object instanceof Boolean) {
      stmt.setBoolean(index, (Boolean)object);
    }
    else if (object instanceof Byte) {
      stmt.setByte(index, (Byte)object);
    }
    else if (object instanceof Short) {
      stmt.setShort(index, (Short)object);
    }
    else if (object instanceof Integer) {
      stmt.setInt(index, (Integer)object);
    }
    else if (object instanceof Long) {
      stmt.setLong(index, (Long)object);
    }
    else if (object instanceof String) {
      stmt.setString(index, (String)object);
    }
    else if (object instanceof java.sql.Date) {
      stmt.setDate(index, (java.sql.Date)object);
    }
    else if (object instanceof java.sql.Timestamp) {
      stmt.setTimestamp(index, (java.sql.Timestamp)object);
    }
    else if (object instanceof java.sql.Time) {
      stmt.setTime(index, (java.sql.Time)object);
    }
    else if (object instanceof java.util.Date) {
      stmt.setTimestamp(index, new Timestamp(((java.util.Date)object).getTime()));
    }
    else {
      boolean assigned = assignSpecificParameter(stmt, index, object);
      if (!assigned) {
        throw new UnhandledTypeError("I don't know how to pass an instance of class " +
                                     object.getClass().getSimpleName() +
                                     " as the " +
                                     index +
                                     "th parameter into a SQL statement.");
      }
    }
  }

  protected boolean assignSpecificParameter(@NotNull final PreparedStatement stmt,
                                            final int index,
                                            @NotNull final Object object) {
    return false;
  }


  @Override
  public BaseScriptRunner script(@NotNull SQLScript script) {
    return new BaseScriptRunner(this, script);
  }


  //// OTHER STUFF \\\\


  @NotNull
  public DBError recognizeError(@NotNull final SQLException sqlException) {
    return facade.getErrorRecognizer().recognizeError(sqlException);
  }


  public void close() {
    if (connectionOwner) {
      try {
        connection.close();
      }
      catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
