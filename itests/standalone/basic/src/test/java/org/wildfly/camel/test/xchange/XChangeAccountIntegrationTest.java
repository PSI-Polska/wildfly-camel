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
package org.wildfly.camel.test.xchange;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.Wallet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@Ignore("FailedToCreateRoute errors")
public class XChangeAccountIntegrationTest extends AbstractXChangeIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-xchange-account-tests");
        archive.addClasses(AbstractXChangeIntegrationTest.class);
        return archive;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBalances() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {

            Assume.assumeTrue(checkAPIConnection());

            camelctx.addRoutes(createRouteBuilder());
            camelctx.start();

            Assume.assumeTrue(hasAPICredentials(camelctx));

            ProducerTemplate template = camelctx.createProducerTemplate();
            List<Balance> balances = template.requestBody("direct:balances", null, List.class);
            Assert.assertNotNull("Balances not null", balances);
            balances.forEach(b -> System.out.println(b));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWallets() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {

            Assume.assumeTrue(checkAPIConnection());

            camelctx.addRoutes(createRouteBuilder());
            camelctx.start();

            Assume.assumeTrue(hasAPICredentials(camelctx));

            ProducerTemplate template = camelctx.createProducerTemplate();
            List<Wallet> wallets = template.requestBody("direct:wallets", null, List.class);
            Assert.assertNotNull("Wallets not null", wallets);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFundingHistory() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {

            Assume.assumeTrue(checkAPIConnection());

            camelctx.addRoutes(createRouteBuilder());
            camelctx.start();

            Assume.assumeTrue(hasAPICredentials(camelctx));

            ProducerTemplate template = camelctx.createProducerTemplate();
            List<FundingRecord> records = template.requestBody("direct:fundingHistory", null, List.class);
            Assert.assertNotNull("Funding records not null", records);
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:balances")
                .to("xchange:binance?service=account&method=balances");

                from("direct:wallets")
                    .to("xchange:binance?service=account&method=wallets");

                from("direct:fundingHistory")
                    .to("xchange:binance?service=account&method=fundingHistory");
            }
        };
    }
}
