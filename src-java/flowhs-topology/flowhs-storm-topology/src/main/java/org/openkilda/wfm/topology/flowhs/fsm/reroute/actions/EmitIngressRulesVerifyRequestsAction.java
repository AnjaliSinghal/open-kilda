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

package org.openkilda.wfm.topology.flowhs.fsm.reroute.actions;

import org.openkilda.wfm.topology.flowhs.fsm.common.actions.HistoryRecordingAction;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteContext;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteFsm;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteFsm.State;
import org.openkilda.wfm.topology.flowhs.utils.SpeakerVerifySegmentEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmitIngressRulesVerifyRequestsAction
        extends HistoryRecordingAction<FlowRerouteFsm, State, Event, FlowRerouteContext> {
    @Override
    public void perform(State from, State to, Event event, FlowRerouteContext context, FlowRerouteFsm stateMachine) {
        stateMachine.getPendingCommands().clear();
        stateMachine.getPendingCommands().addAll(SpeakerVerifySegmentEmitter.INSTANCE.emitBatchKeepingCommandId(
                stateMachine.getCarrier(), stateMachine.getIngressCommands()));
        stateMachine.getRetriedCommands().clear();

        stateMachine.saveActionToHistory("Started validation of installed ingress rules");
    }
}
