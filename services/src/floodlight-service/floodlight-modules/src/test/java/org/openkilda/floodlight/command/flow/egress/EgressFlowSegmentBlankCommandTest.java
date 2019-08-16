/* Copyright 2019 Telstra Open Source
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

package org.openkilda.floodlight.command.flow.egress;

import org.openkilda.floodlight.api.FlowEndpoint;
import org.openkilda.floodlight.api.FlowTransitEncapsulation;
import org.openkilda.floodlight.command.AbstractSpeakerCommandTest;
import org.openkilda.floodlight.command.flow.FlowSegmentReport;
import org.openkilda.floodlight.error.SwitchErrorResponseException;
import org.openkilda.floodlight.error.SwitchOperationException;
import org.openkilda.model.FlowEncapsulationType;
import org.openkilda.model.SwitchId;

import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFBadRequestCode;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.concurrent.CompletableFuture;

abstract class EgressFlowSegmentBlankCommandTest extends AbstractSpeakerCommandTest {
    protected final static DatapathId ingresDpId = DatapathId.of(dpId.getLong() + 1);

    protected static final FlowTransitEncapsulation encapsulationVlan = new FlowTransitEncapsulation(
            51, FlowEncapsulationType.TRANSIT_VLAN);

    protected final static FlowEndpoint endpointZeroPort = new FlowEndpoint(mapSwitchId(dpId), 11, 0, 0);
    protected final static FlowEndpoint endpointSingleVlan = new FlowEndpoint(mapSwitchId(dpId), 12, 64, 0);
    protected final static FlowEndpoint endpointDoubleVlan = new FlowEndpoint(mapSwitchId(dpId), 13, 64, 65);

    @Test
    public void errorOnFlowMod() {
        switchFeaturesSetup(true);
        replayAll();

        FlowEndpoint ingressEndpoint = new FlowEndpoint(mapSwitchId(ingresDpId), 1, 0, 0);
        EgressFlowSegmentBlankCommand command = makeCommand(endpointZeroPort, ingressEndpoint, encapsulationVlan);
        CompletableFuture<FlowSegmentReport> result = command.execute(commandProcessor);

        getWriteRecord(0).getFuture()
                .completeExceptionally(new SwitchErrorResponseException(
                        dpId, of.errorMsgs().buildBadRequestErrorMsg().setCode(OFBadRequestCode.BAD_LEN).build()));
        verifyErrorCompletion(result, SwitchOperationException.class);
    }

    protected void executeCommand(EgressFlowSegmentBlankCommand command, int writeCount) throws Exception {
        replayAll();

        final CompletableFuture<FlowSegmentReport> result = command.execute(commandProcessor);
        verifyWriteCount(writeCount);
        verifySuccessCompletion(result);
    }

    protected abstract EgressFlowSegmentBlankCommand makeCommand(
            FlowEndpoint endpoint, FlowEndpoint ingressEndpoint, FlowTransitEncapsulation encapsulation);
}
