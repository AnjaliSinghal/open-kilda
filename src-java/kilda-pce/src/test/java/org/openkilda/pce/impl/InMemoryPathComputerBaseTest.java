/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.pce.impl;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.openkilda.config.provider.ConfigurationProvider;
import org.openkilda.config.provider.PropertiesBasedConfigurationProvider;
import org.openkilda.model.Flow;
import org.openkilda.model.FlowEncapsulationType;
import org.openkilda.model.FlowPath;
import org.openkilda.model.Isl;
import org.openkilda.model.IslStatus;
import org.openkilda.model.PathComputationStrategy;
import org.openkilda.model.PathId;
import org.openkilda.model.PathSegment;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchId;
import org.openkilda.model.SwitchProperties;
import org.openkilda.model.SwitchStatus;
import org.openkilda.pce.AvailableNetworkFactory;
import org.openkilda.pce.Path;
import org.openkilda.pce.PathComputer;
import org.openkilda.pce.PathComputerConfig;
import org.openkilda.pce.PathComputerFactory;
import org.openkilda.pce.PathPair;
import org.openkilda.pce.exception.RecoverableException;
import org.openkilda.pce.exception.UnroutableFlowException;
import org.openkilda.pce.finder.BestWeightAndShortestPathFinder;
import org.openkilda.persistence.Neo4jConfig;
import org.openkilda.persistence.NetworkConfig;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.repositories.FlowPathRepository;
import org.openkilda.persistence.repositories.FlowRepository;
import org.openkilda.persistence.repositories.IslRepository;
import org.openkilda.persistence.repositories.RepositoryFactory;
import org.openkilda.persistence.repositories.SwitchPropertiesRepository;
import org.openkilda.persistence.repositories.SwitchRepository;
import org.openkilda.persistence.repositories.impl.Neo4jSessionFactory;
import org.openkilda.persistence.spi.PersistenceProvider;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.ogm.testutil.TestServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryPathComputerBaseTest {

    static TestServer testServer;
    static TransactionManager txManager;
    static SwitchRepository switchRepository;
    static SwitchPropertiesRepository switchPropertiesRepository;
    static IslRepository islRepository;
    static FlowRepository flowRepository;
    static FlowPathRepository flowPathRepository;

    static PathComputerConfig config;

    static AvailableNetworkFactory availableNetworkFactory;
    static PathComputerFactory pathComputerFactory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpOnce() {
        testServer = new TestServer(true, true, 5);

        PersistenceManager persistenceManager = PersistenceProvider.getInstance().createPersistenceManager(
                new ConfigurationProvider() { //NOSONAR
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T getConfiguration(Class<T> configurationType) {
                        if (configurationType.equals(Neo4jConfig.class)) {
                            return (T) new Neo4jConfig() {
                                @Override
                                public String getUri() {
                                    return testServer.getUri();
                                }

                                @Override
                                public String getLogin() {
                                    return testServer.getUsername();
                                }

                                @Override
                                public String getPassword() {
                                    return testServer.getPassword();
                                }

                                @Override
                                public int getConnectionPoolSize() {
                                    return 50;
                                }

                                @Override
                                public String getIndexesAuto() {
                                    return "update";
                                }
                            };
                        } else if (configurationType.equals(NetworkConfig.class)) {
                            return (T) new NetworkConfig() {
                                @Override
                                public int getIslUnstableTimeoutSec() {
                                    return 7200;
                                }
                            };
                        } else {
                            throw new UnsupportedOperationException("Unsupported configurationType "
                                    + configurationType);
                        }
                    }
                });

        txManager = persistenceManager.getTransactionManager();

        RepositoryFactory repositoryFactory = persistenceManager.getRepositoryFactory();
        switchRepository = repositoryFactory.createSwitchRepository();
        switchPropertiesRepository = repositoryFactory.createSwitchPropertiesRepository();
        islRepository = repositoryFactory.createIslRepository();
        flowRepository = repositoryFactory.createFlowRepository();
        flowPathRepository = repositoryFactory.createFlowPathRepository();

        config = new PropertiesBasedConfigurationProvider().getConfiguration(PathComputerConfig.class);

        availableNetworkFactory = new AvailableNetworkFactory(config, repositoryFactory);
        pathComputerFactory = new PathComputerFactory(config, availableNetworkFactory);
    }

    @AfterClass
    public static void tearDown() {
        testServer.shutdown();
    }

    @After
    public void cleanUp() {
        ((Neo4jSessionFactory) txManager).getSession().purgeDatabase();
    }

    /**
     * See how it works with a large network. It takes a while to create the network .. therefore @Ignore so that it
     * doesn't slow down unit tests.
     */
    @Test
    @Ignore
    public void shouldFindOverLargeIslands() throws RecoverableException, UnroutableFlowException {
        createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 10, 20, "08:", 1);

        for (int i = 0; i < 50; i++) {
            createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 10, 20, "10:", 4 * i + 1);
            createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 10, 20, "11:", 4 * i + 1);
            createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 10, 20, "12:", 4 * i + 1);
            createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 10, 20, "13:", 4 * i + 1);
        }
        for (int i = 0; i < 49; i++) {
            String prev = format("%02X", 4 * i + 4);
            String next = format("%02X", 4 * i + 5);
            connectDiamonds(new SwitchId("10:" + prev), new SwitchId("10:" + next), IslStatus.ACTIVE, 20, 50);
            connectDiamonds(new SwitchId("11:" + prev), new SwitchId("11:" + next), IslStatus.ACTIVE, 20, 50);
            connectDiamonds(new SwitchId("12:" + prev), new SwitchId("12:" + next), IslStatus.ACTIVE, 20, 50);
            connectDiamonds(new SwitchId("13:" + prev), new SwitchId("13:" + next), IslStatus.ACTIVE, 20, 50);
        }
        connectDiamonds(new SwitchId("10:99"), new SwitchId("11:22"), IslStatus.ACTIVE, 20, 50);
        connectDiamonds(new SwitchId("11:99"), new SwitchId("12:22"), IslStatus.ACTIVE, 20, 50);
        connectDiamonds(new SwitchId("12:99"), new SwitchId("13:22"), IslStatus.ACTIVE, 20, 50);
        connectDiamonds(new SwitchId("13:99"), new SwitchId("10:22"), IslStatus.ACTIVE, 20, 50);

        Switch srcSwitch1 = getSwitchById("10:01");
        Switch destSwitch1 = getSwitchById("11:03");

        // THIS ONE SHOULD WORK
        Flow f1 = new TestFlowBuilder()
                .srcSwitch(srcSwitch1)
                .destSwitch(destSwitch1)
                .bandwidth(0)
                .ignoreBandwidth(false)
                .build();

        PathComputer pathComputer = new InMemoryPathComputer(availableNetworkFactory,
                new BestWeightAndShortestPathFinder(200), config);
        PathPair path = pathComputer.getPath(f1);
        assertNotNull(path);
        assertThat(path.getForward().getSegments(), Matchers.hasSize(278));

        Switch srcSwitch2 = getSwitchById("08:01");
        Switch destSwitch2 = getSwitchById("11:04");

        // THIS ONE SHOULD FAIL
        Flow f2 = new TestFlowBuilder()
                .srcSwitch(srcSwitch2)
                .destSwitch(destSwitch2)
                .bandwidth(0)
                .ignoreBandwidth(false)
                .build();

        thrown.expect(UnroutableFlowException.class);

        pathComputer.getPath(f2);
    }

    /**
     * This verifies that the getPath in PathComputer returns what we expect. Essentially, this tests the additional
     * logic wrt taking the results of the algo and convert to something installable.
     */
    @Test
    public void verifyConversionToPair() throws UnroutableFlowException, RecoverableException {
        createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 10, 20, "09:", 1);

        Switch srcSwitch = getSwitchById("09:01");
        Switch destSwitch = getSwitchById("09:04");

        Flow flow = new TestFlowBuilder()
                .srcSwitch(srcSwitch)
                .destSwitch(destSwitch)
                .bandwidth(10)
                .ignoreBandwidth(false)
                .build();

        PathComputer pathComputer = pathComputerFactory.getPathComputer();
        PathPair result = pathComputer.getPath(flow);
        assertNotNull(result);
        // ensure start/end switches match
        List<Path.Segment> left = result.getForward().getSegments();
        assertEquals(srcSwitch.getSwitchId(), left.get(0).getSrcSwitchId());
        assertEquals(destSwitch.getSwitchId(), left.get(left.size() - 1).getDestSwitchId());

        List<Path.Segment> right = result.getReverse().getSegments();
        assertEquals(destSwitch.getSwitchId(), right.get(0).getSrcSwitchId());
        assertEquals(srcSwitch.getSwitchId(), right.get(right.size() - 1).getDestSwitchId());
    }

    /**
     * Checks that existed flow should always have available path even there is only links with 0 available bandwidth.
     */
    @Test
    public void shouldAlwaysFindPathForExistedFlow() throws RecoverableException, UnroutableFlowException {
        String flowId = "flow-A1:01-A1:03";
        long bandwidth = 1000;

        createLinearTopoWithFlowSegments(10, "A1:", 1, 0L,
                flowId, bandwidth);

        Switch srcSwitch = getSwitchById("A1:01");
        Switch destSwitch = getSwitchById("A1:03");

        Flow flow = new TestFlowBuilder()
                .flowId(flowId)
                .srcSwitch(srcSwitch)
                .destSwitch(destSwitch)
                .bandwidth(bandwidth)
                .ignoreBandwidth(false)
                .build();
        Flow oldFlow = flowRepository.findById(flowId).orElseThrow(() -> new AssertionError("Flow not found"));

        PathComputer pathComputer = pathComputerFactory.getPathComputer();
        PathPair result = pathComputer.getPath(flow, oldFlow.getFlowPathIds());

        assertThat(result.getForward().getSegments(), Matchers.hasSize(2));
        assertThat(result.getReverse().getSegments(), Matchers.hasSize(2));
    }

    /**
     * Tests the case when we try to increase bandwidth of the flow and there is no available bandwidth left.
     */
    @Test
    public void shouldNotFindPathForExistedFlowAndIncreasedBandwidth()
            throws RecoverableException, UnroutableFlowException {
        String flowId = "flow-A1:01-A1:03";
        long originFlowBandwidth = 1000L;

        // create network, all links have available bandwidth 0
        createLinearTopoWithFlowSegments(10, "A1:", 1, 0,
                flowId, originFlowBandwidth);

        Switch srcSwitch = getSwitchById("A1:01");
        Switch destSwitch = getSwitchById("A1:03");

        Flow flow = new TestFlowBuilder()
                .flowId(flowId)
                .srcSwitch(srcSwitch)
                .destSwitch(destSwitch)
                .bandwidth(originFlowBandwidth)
                .ignoreBandwidth(false)
                .build();

        long updatedFlowBandwidth = originFlowBandwidth + 1;
        flow.setBandwidth(updatedFlowBandwidth);

        thrown.expect(UnroutableFlowException.class);

        PathComputer pathComputer = pathComputerFactory.getPathComputer();
        pathComputer.getPath(flow, flow.getFlowPathIds());
    }

    /**
     * Special case: flow with MAX_LATENCY strategy and 'max-latency' set to 0 should pick path with least latency.
     */
    @Test
    public void maxLatencyStratWithZeroLatency() throws RecoverableException, UnroutableFlowException {
        // 1 - 2 - 4
        //   + 3 +
        //path 1>2>4 has less latency than 1>3>4
        createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 100, 100, "00:", 1, 100, 101);
        //when: request a flow with MAX_LATENCY strategy and 'max-latency' set to 0
        Flow flow = Flow.builder()
                .flowId("test flow")
                .srcSwitch(getSwitchById("00:01")).srcPort(15)
                .destSwitch(getSwitchById("00:04")).destPort(15)
                .bandwidth(500)
                .encapsulationType(FlowEncapsulationType.TRANSIT_VLAN)
                .pathComputationStrategy(PathComputationStrategy.MAX_LATENCY)
                .maxLatency(0L)
                .build();
        PathComputer pathComputer = new InMemoryPathComputer(availableNetworkFactory,
                new BestWeightAndShortestPathFinder(5), config);
        PathPair paths = pathComputer.getPath(flow);

        //then: system returns a path with least weight
        assertThat(paths.getForward().getSegments().get(1).getSrcSwitchId(), equalTo(new SwitchId("00:02")));
        assertThat(paths.getReverse().getSegments().get(1).getSrcSwitchId(), equalTo(new SwitchId("00:02")));
    }

    /**
     * Special case: flow with MAX_LATENCY strategy and 'max-latency' being unset(null) should pick path with least
     * latency.
     */
    @Test
    public void maxLatencyStratWithNullLatency() throws RecoverableException, UnroutableFlowException {
        // 1 - 2 - 4
        //   + 3 +
        //path 1>2>4 has less latency than 1>3>4
        createDiamond(IslStatus.ACTIVE, IslStatus.ACTIVE, 100, 100, "00:", 1, 100, 101);
        //when: request a flow with MAX_LATENCY strategy and 'max-latency' set to 0
        Flow flow = Flow.builder()
                .flowId("test flow")
                .srcSwitch(getSwitchById("00:01")).srcPort(15)
                .destSwitch(getSwitchById("00:04")).destPort(15)
                .bandwidth(500)
                .encapsulationType(FlowEncapsulationType.TRANSIT_VLAN)
                .pathComputationStrategy(PathComputationStrategy.MAX_LATENCY)
                .build();
        PathComputer pathComputer = new InMemoryPathComputer(availableNetworkFactory,
                new BestWeightAndShortestPathFinder(5), config);
        PathPair paths = pathComputer.getPath(flow);

        //then: system returns a path with least weight
        assertThat(paths.getForward().getSegments().get(1).getSrcSwitchId(), equalTo(new SwitchId("00:02")));
        assertThat(paths.getReverse().getSegments().get(1).getSrcSwitchId(), equalTo(new SwitchId("00:02")));
    }

    void addPathSegments(FlowPath flowPath, Path path) {
        path.getSegments().forEach(segment ->
                addPathSegment(flowPath, switchRepository.findById(segment.getSrcSwitchId()).get(),
                        switchRepository.findById(segment.getDestSwitchId()).get(),
                        segment.getSrcPort(), segment.getDestPort()));
    }

    private void createLinearTopoWithFlowSegments(int cost, String switchStart, int startIndex, long linkBw,
                                                  String flowId, long flowBandwidth) {
        // A - B - C
        int index = startIndex;

        Switch nodeA = createSwitch(switchStart + format("%02X", index++));
        Switch nodeB = createSwitch(switchStart + format("%02X", index++));
        Switch nodeC = createSwitch(switchStart + format("%02X", index));

        createIsl(nodeA, nodeB, IslStatus.ACTIVE, IslStatus.ACTIVE, cost, linkBw, 5);
        createIsl(nodeB, nodeC, IslStatus.ACTIVE, IslStatus.ACTIVE, cost, linkBw, 6);
        createIsl(nodeC, nodeB, IslStatus.ACTIVE, IslStatus.ACTIVE, cost, linkBw, 6);
        createIsl(nodeB, nodeA, IslStatus.ACTIVE, IslStatus.ACTIVE, cost, linkBw, 5);

        Flow flow = Flow.builder()
                .flowId(flowId)
                .srcSwitch(nodeA)
                .destSwitch(nodeC)
                .build();
        flowRepository.createOrUpdate(flow);

        FlowPath forwardPath = FlowPath.builder()
                .pathId(new PathId(UUID.randomUUID().toString()))
                .srcSwitch(nodeA)
                .destSwitch(nodeC)
                .flow(flow)
                .bandwidth(flowBandwidth)
                .segments(new ArrayList<>())
                .build();
        addPathSegment(forwardPath, nodeA, nodeB, 5, 5);
        addPathSegment(forwardPath, nodeB, nodeC, 6, 6);
        flowPathRepository.createOrUpdate(forwardPath);

        FlowPath reversePath = FlowPath.builder()
                .pathId(new PathId(UUID.randomUUID().toString()))
                .srcSwitch(nodeC)
                .destSwitch(nodeA)
                .flow(flow)
                .bandwidth(flowBandwidth)
                .segments(new ArrayList<>())
                .build();
        addPathSegment(reversePath, nodeC, nodeB, 6, 6);
        addPathSegment(reversePath, nodeB, nodeA, 5, 5);
        flowPathRepository.createOrUpdate(reversePath);
    }

    // A - B - D    and A-B-D is used in flow group
    //   + C +
    void createDiamondWithDiversity() {
        Switch nodeA = createSwitch("00:0A");
        Switch nodeB = createSwitch("00:0B");
        Switch nodeC = createSwitch("00:0C");
        Switch nodeD = createSwitch("00:0D");

        IslStatus status = IslStatus.ACTIVE;
        int cost = 100;
        createIsl(nodeA, nodeB, status, status, cost, 1000, 1);
        createIsl(nodeA, nodeC, status, status, cost * 2, 1000, 2);
        createIsl(nodeB, nodeD, status, status, cost, 1000, 3);
        createIsl(nodeC, nodeD, status, status, cost * 2, 1000, 4);
        createIsl(nodeB, nodeA, status, status, cost, 1000, 1);
        createIsl(nodeC, nodeA, status, status, cost * 2, 1000, 2);
        createIsl(nodeD, nodeB, status, status, cost, 1000, 3);
        createIsl(nodeD, nodeC, status, status, cost * 2, 1000, 4);

        int bandwith = 10;
        String flowId = "existed-flow";

        String groupId = "diverse";

        Flow flow = Flow.builder()
                .flowId(flowId)
                .srcSwitch(nodeA).srcPort(15)
                .destSwitch(nodeD).destPort(16)
                .groupId(groupId)
                .bandwidth(bandwith)
                .encapsulationType(FlowEncapsulationType.TRANSIT_VLAN)
                .build();

        FlowPath forwardPath = FlowPath.builder()
                .pathId(new PathId(UUID.randomUUID().toString()))
                .srcSwitch(nodeA)
                .destSwitch(nodeD)
                .flow(flow)
                .bandwidth(bandwith)
                .segments(new ArrayList<>())
                .build();
        addPathSegment(forwardPath, nodeA, nodeB, 1, 1);
        addPathSegment(forwardPath, nodeB, nodeD, 3, 3);
        flow.setForwardPath(forwardPath);

        FlowPath reversePath = FlowPath.builder()
                .pathId(new PathId(UUID.randomUUID().toString()))
                .srcSwitch(nodeD)
                .destSwitch(nodeA)
                .flow(flow)
                .bandwidth(bandwith)
                .segments(new ArrayList<>())
                .build();
        addPathSegment(reversePath, nodeD, nodeB, 3, 3);
        addPathSegment(reversePath, nodeB, nodeA, 1, 1);
        flow.setReversePath(reversePath);

        flowRepository.createOrUpdate(flow);
    }

    void createDiamond(IslStatus pathBstatus, IslStatus pathCstatus, int pathBcost, int pathCcost,
                       String switchStart, int startIndex) {
        createDiamond(pathBstatus, pathCstatus, pathBcost, pathCcost, switchStart, startIndex, 5L, 5L);
    }

    void createDiamond(IslStatus pathBstatus, IslStatus pathCstatus, int pathBcost, int pathCcost,
                       String switchStart, int startIndex, long pathBlatency, long pathClatency) {
        // A - B - D
        //   + C +
        int index = startIndex;

        Switch nodeA = createSwitch(switchStart + format("%02X", index++));
        Switch nodeB = createSwitch(switchStart + format("%02X", index++));
        Switch nodeC = createSwitch(switchStart + format("%02X", index++));
        Switch nodeD = createSwitch(switchStart + format("%02X", index));

        IslStatus actual = (pathBstatus == IslStatus.ACTIVE) && (pathCstatus == IslStatus.ACTIVE)
                ? IslStatus.ACTIVE : IslStatus.INACTIVE;
        createIsl(nodeA, nodeB, pathBstatus, actual, pathBcost, 1000, 5, pathBlatency);
        createIsl(nodeA, nodeC, pathCstatus, actual, pathCcost, 1000, 6, pathClatency);
        createIsl(nodeB, nodeD, pathBstatus, actual, pathBcost, 1000, 6, pathBlatency);
        createIsl(nodeC, nodeD, pathCstatus, actual, pathCcost, 1000, 5, pathClatency);
        createIsl(nodeB, nodeA, pathBstatus, actual, pathBcost, 1000, 5, pathBlatency);
        createIsl(nodeC, nodeA, pathCstatus, actual, pathCcost, 1000, 6, pathClatency);
        createIsl(nodeD, nodeB, pathBstatus, actual, pathBcost, 1000, 6, pathBlatency);
        createIsl(nodeD, nodeC, pathCstatus, actual, pathCcost, 1000, 5, pathClatency);
    }

    private void connectDiamonds(SwitchId switchA, SwitchId switchB, IslStatus status, int cost, int port) {
        // A - B - D
        //   + C +
        Switch nodeA = switchRepository.findById(switchA).get();
        Switch nodeB = switchRepository.findById(switchB).get();
        createIsl(nodeA, nodeB, status, status, cost, 1000, port);
        createIsl(nodeB, nodeA, status, status, cost, 1000, port);
    }

    void createTriangleTopo(IslStatus pathABstatus, int pathABcost, int pathCcost,
                            String switchStart, int startIndex) {
        // A - B
        // + C +
        int index = startIndex;

        Switch nodeA = createSwitch(switchStart + format("%02X", index++));
        Switch nodeB = createSwitch(switchStart + format("%02X", index++));
        Switch nodeC = createSwitch(switchStart + format("%02X", index));

        createIsl(nodeA, nodeB, pathABstatus, pathABstatus, pathABcost, 1000, 5, 1000L);
        createIsl(nodeB, nodeA, pathABstatus, pathABstatus, pathABcost, 1000, 5, 1000L);
        createIsl(nodeA, nodeC, IslStatus.ACTIVE, IslStatus.ACTIVE, pathCcost, 1000, 6, 100L);
        createIsl(nodeC, nodeA, IslStatus.ACTIVE, IslStatus.ACTIVE, pathCcost, 1000, 6, 100L);
        createIsl(nodeC, nodeB, IslStatus.ACTIVE, IslStatus.ACTIVE, pathCcost, 1000, 7, 100L);
        createIsl(nodeB, nodeC, IslStatus.ACTIVE, IslStatus.ACTIVE, pathCcost, 1000, 7, 100L);
    }

    private Switch createSwitch(String name) {
        Switch sw = Switch.builder().switchId(new SwitchId(name)).status(SwitchStatus.ACTIVE)
                .build();

        switchRepository.createOrUpdate(sw);
        SwitchProperties switchProperties = SwitchProperties.builder().switchObj(sw)
                .supportedTransitEncapsulation(SwitchProperties.DEFAULT_FLOW_ENCAPSULATION_TYPES).build();
        switchPropertiesRepository.createOrUpdate(switchProperties);
        return sw;
    }

    private void createIsl(Switch srcSwitch, Switch dstSwitch, IslStatus status, IslStatus actual,
                           int cost, long bw, int port) {
        createIsl(srcSwitch, dstSwitch, status, actual, cost, bw, port, 5L);
    }

    private void createIsl(Switch srcSwitch, Switch dstSwitch, IslStatus status, IslStatus actual,
                           int cost, long bw, int port, long latency) {
        Isl isl = new Isl();
        isl.setSrcSwitch(srcSwitch);
        isl.setDestSwitch(dstSwitch);
        isl.setStatus(status);
        isl.setActualStatus(actual);
        if (cost >= 0) {
            isl.setCost(cost);
        }
        isl.setAvailableBandwidth(bw);
        isl.setLatency(latency);
        isl.setSrcPort(port);
        isl.setDestPort(port);

        islRepository.createOrUpdate(isl);
    }

    private void addPathSegment(FlowPath flowPath, Switch src, Switch dst, int srcPort, int dstPort) {
        PathSegment ps = new PathSegment();
        ps.setSrcSwitch(src);
        ps.setDestSwitch(dst);
        ps.setSrcPort(srcPort);
        ps.setDestPort(dstPort);
        ps.setLatency(null);
        List<PathSegment> segments = new ArrayList<>(flowPath.getSegments());
        segments.add(ps);
        flowPath.setSegments(segments);
    }

    Switch getSwitchById(String id) {
        return switchRepository.findById(new SwitchId(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Switch %s not found", id)));
    }
}
