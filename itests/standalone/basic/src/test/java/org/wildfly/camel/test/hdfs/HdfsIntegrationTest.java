/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.hdfs;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ HdfsIntegrationTest.ServerSetup.class })
@Ignore("[#2977] Restore hdfs testing for Camel-3.4.x")
public class HdfsIntegrationTest {

    static class ServerSetup implements ServerSetupTask {

        private MiniDFSCluster hdfsCluster;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            if (!EnvironmentUtils.isWindows()) {
                String dataDir = Paths.get(System.getProperty("jboss.home.dir"), "standalone", "data", "hadoop").toString();

                Configuration configuration = new Configuration();
                configuration.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, dataDir);

                MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(configuration);
                hdfsCluster = builder.build();

                AvailablePortFinder.storeServerData("hdfs-port", hdfsCluster.getNameNodePort());
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (!EnvironmentUtils.isWindows()) {
                hdfsCluster.shutdown(true);
            }
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-hdfs-tests.jar")
            .addClasses(AvailablePortFinder.class, EnvironmentUtils.class);
    }

    @Test
    public void testHdfsComponent() throws Exception {

        Assume.assumeFalse("[#1961] HdfsIntegrationTest fails on Windows", EnvironmentUtils.isWindows());

        String dataDir = Paths.get(System.getProperty("jboss.server.data.dir"), "hadoop").toString();
        String port = AvailablePortFinder.readServerData("hdfs-port");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("hdfs://localhost:%s%s?fileSystemType=HDFS&splitStrategy=BYTES:5,IDLE:1000", port, dataDir);

                fromF("hdfs://localhost:%s%s?pattern=*&fileSystemType=HDFS&chunkSize=5", port, dataDir).id("hdfs-consumer").autoStartup(false)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            List<String> bodies = new ArrayList<>();
            for (int i = 0; i < 10; ++i) {
                String body = "CIAO" + i;
                bodies.add(body);
                template.sendBody("direct:start", body);
            }

            camelctx.getRouteController().startRoute("hdfs-consumer");

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceivedInAnyOrder(bodies);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }
}
