package org.jetbrains.dba.access;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.dba.DBTestCase;
import org.jetbrains.dba.sql.SQL;
import org.jetbrains.dba.sql.SQLScript;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;



/**
 * @author Leonid Bushuev from JetBrains
 */
@Test(groups = "all-db")
public class BaseScriptRunnerTest extends DBTestCase {

  private static SQL ourCommonSQL;


  @BeforeClass
  public void setUp2() throws Exception {
    myFacade.connect();
    ourCommonSQL = new SQL();
    ourCommonSQL.assignResources(BaseScriptRunnerTest.class.getClassLoader(), "common");
  }


  @Test
  public void simple_script() {
    final SQLScript script = ourCommonSQL.script("##simple-script");

    myFacade.inSession(new InSessionNoResult() {
      @Override
      public void run(@NotNull DBSession session) {

        session.script(script).run();

      }
    });

  }


}
