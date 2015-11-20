/*
 * This file is part of Gradoop.
 *
 * Gradoop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gradoop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Gradoop.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gradoop.model;

import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.StreamingMode;
import org.apache.flink.test.util.ForkableFlinkMiniCluster;
import org.apache.flink.test.util.MultipleProgramsTestBase;
import org.gradoop.GradoopTestBaseUtils;
import org.gradoop.model.impl.pojo.EdgePojo;
import org.gradoop.model.impl.pojo.GraphHeadPojo;
import org.gradoop.model.impl.pojo.VertexPojo;
import org.gradoop.model.impl.EPGMDatabase;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Used for tests that require a Flink cluster up and running.
 */
public class FlinkTestBase extends MultipleProgramsTestBase {

  private EPGMDatabase<VertexPojo, EdgePojo, GraphHeadPojo>
    graphStore;

  private ExecutionEnvironment env;

  public FlinkTestBase(TestExecutionMode mode) {
    super(mode);
    this.env = ExecutionEnvironment.getExecutionEnvironment();
    this.graphStore = createSocialGraph();
  }

  /**
   * Creates a social network as a basis for tests.
   * <p/>
   * An image of the network can be found in
   * gradoop/dev-support/social-network.pdf
   *
   * @return graph store containing a simple social network for tests.
   */
  protected EPGMDatabase<VertexPojo, EdgePojo, GraphHeadPojo> createSocialGraph() {
    return EPGMDatabase
      .fromCollection(GradoopTestBaseUtils.createVertexPojoCollection(),
        GradoopTestBaseUtils.createEdgePojoCollection(),
        GradoopTestBaseUtils.createGraphHeadCollection(), env);
  }

  protected EPGMDatabase<VertexPojo, EdgePojo, GraphHeadPojo> getGraphStore() {
    return graphStore;
  }

  protected ExecutionEnvironment getExecutionEnvironment() {
    return env;
  }

  /**
   * Custom test cluster start routine,
   * workaround to set TASK_MANAGER_MEMORY_SIZE.
   *
   * @throws Exception
   */
  @BeforeClass
  public static void setup() throws Exception {

    logDir = File.createTempFile("TestBaseUtils-logdir", null);
    Assert.assertTrue("Unable to delete temp file", logDir.delete());
    Assert.assertTrue("Unable to create temp directory", logDir.mkdir());
    Path logFile = Files.createFile(new File(logDir, "jobmanager.log").toPath());
    Files.createFile(new File(logDir, "jobmanager.out").toPath());

    Configuration config = new Configuration();

    config.setInteger(
      ConfigConstants.LOCAL_NUMBER_TASK_MANAGER, 1);
    config.setInteger(
      ConfigConstants.TASK_MANAGER_NUM_TASK_SLOTS, 4);

    config.setBoolean(
      ConfigConstants.LOCAL_START_WEBSERVER, startWebServer);

    config.setLong(
      ConfigConstants.TASK_MANAGER_MEMORY_SIZE_KEY, 128L);
    config.setBoolean(
      ConfigConstants.FILESYSTEM_DEFAULT_OVERWRITE_KEY, true);

    config.setString(
      ConfigConstants.AKKA_ASK_TIMEOUT, DEFAULT_AKKA_ASK_TIMEOUT + "s");
    config.setString(
      ConfigConstants.AKKA_STARTUP_TIMEOUT, DEFAULT_AKKA_STARTUP_TIMEOUT);

    config.setInteger(
      ConfigConstants.JOB_MANAGER_WEB_PORT_KEY, 8081);
    config.setString(
      ConfigConstants.JOB_MANAGER_WEB_LOG_PATH_KEY, logFile.toString());

    ForkableFlinkMiniCluster cluster =
      new ForkableFlinkMiniCluster(config, true, StreamingMode.BATCH_ONLY);

    cluster.start();

    MultipleProgramsTestBase.cluster = cluster;
  }

}
