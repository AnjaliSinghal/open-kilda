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

package org.openkilda.wfm.topology.flowhs.fsm.create.action;

import static java.lang.String.format;

import org.openkilda.model.FlowStatus;
import org.openkilda.wfm.share.logger.FlowOperationsDashboardLogger;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.HistoryRecordingAction;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.State;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlow;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnFinishedAction extends HistoryRecordingAction<FlowCreateFsm, State, Event, FlowCreateContext> {
    private final FlowOperationsDashboardLogger dashboardLogger;

    public OnFinishedAction(FlowOperationsDashboardLogger dashboardLogger) {
        this.dashboardLogger = dashboardLogger;
    }

    @Override
    public void perform(State from, State to, Event event, FlowCreateContext context, FlowCreateFsm stateMachine) {
        if (stateMachine.getFlowStatus() == FlowStatus.UP) {
            sendPeriodicPingNotification(stateMachine);
            dashboardLogger.onSuccessfulFlowCreate(stateMachine.getFlowId());
            stateMachine.saveActionToHistory("Flow was created successfully");
        } else if (stateMachine.getFlowStatus() == FlowStatus.DEGRADED) {
            sendPeriodicPingNotification(stateMachine);
            dashboardLogger.onFailedFlowCreate(stateMachine.getFlowId(), "Protected path not found");
            stateMachine.saveActionToHistory("Main flow path created successfully but no protected path found");
        } else {
            stateMachine.saveActionToHistory("Flow create completed",
                    format("Flow create completed with status %s and error %s", stateMachine.getFlowStatus(),
                            stateMachine.getErrorReason()));
        }
    }

    private void sendPeriodicPingNotification(FlowCreateFsm stateMachine) {
        RequestedFlow requestedFlow = stateMachine.getTargetFlow();
        stateMachine.getCarrier().sendPeriodicPingNotification(requestedFlow.getFlowId(),
                requestedFlow.isPeriodicPings());
    }
}
