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

package org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions;

import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.FlowSwapEndpointsContext;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.FlowSwapEndpointsFsm;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.FlowSwapEndpointsFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.FlowSwapEndpointsFsm.State;

import lombok.extern.slf4j.Slf4j;
import org.squirrelframework.foundation.fsm.AnonymousAction;

@Slf4j
public class ReceivingUpdateResponsesCompletedAction
        extends AnonymousAction<FlowSwapEndpointsFsm, State, Event, FlowSwapEndpointsContext> {

    @Override
    public void execute(State from, State to, Event event, FlowSwapEndpointsContext context,
                        FlowSwapEndpointsFsm stateMachine) {
        if (stateMachine.getFlowResponses().size() == FlowSwapEndpointsFsm.REQUEST_COUNT) {
            stateMachine.fire(Event.NEXT);
        } else {
            stateMachine.fire(Event.REVERT);
        }
    }
}
