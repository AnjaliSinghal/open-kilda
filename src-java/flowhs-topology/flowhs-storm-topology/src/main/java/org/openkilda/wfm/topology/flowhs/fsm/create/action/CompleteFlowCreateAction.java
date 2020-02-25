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

import org.openkilda.messaging.error.ErrorType;
import org.openkilda.model.FlowPathStatus;
import org.openkilda.model.FlowStatus;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.wfm.share.logger.FlowOperationsDashboardLogger;
import org.openkilda.wfm.topology.flowhs.exception.FlowProcessingException;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.FlowProcessingAction;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.create.FlowCreateFsm.State;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlow;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompleteFlowCreateAction extends FlowProcessingAction<FlowCreateFsm, State, Event, FlowCreateContext> {
    private final FlowOperationsDashboardLogger dashboardLogger;

    public CompleteFlowCreateAction(PersistenceManager persistenceManager,
                                    FlowOperationsDashboardLogger dashboardLogger) {
        super(persistenceManager);
        this.dashboardLogger = dashboardLogger;
    }

    @Override
    protected void perform(State from, State to, Event event, FlowCreateContext context, FlowCreateFsm stateMachine) {
        persistenceManager.getTransactionManager().doInTransaction(() -> {
            String flowId = stateMachine.getFlowId();
            if (!flowRepository.exists(flowId)) {
                throw new FlowProcessingException(ErrorType.NOT_FOUND,
                        "Couldn't complete flow creation. The flow was deleted");
            }

            RequestedFlow flow = stateMachine.getTargetFlow();
            flowPathRepository.updateStatus(stateMachine.getForwardPathId(), FlowPathStatus.ACTIVE);
            flowPathRepository.updateStatus(stateMachine.getReversePathId(), FlowPathStatus.ACTIVE);
            FlowStatus flowStatus = flow.isAllocateProtectedPath() ? FlowStatus.DEGRADED : FlowStatus.UP;
            if (stateMachine.getProtectedForwardPathId() != null && stateMachine.getProtectedReversePathId() != null) {
                flowPathRepository.updateStatus(stateMachine.getProtectedForwardPathId(), FlowPathStatus.ACTIVE);
                flowPathRepository.updateStatus(stateMachine.getProtectedReversePathId(), FlowPathStatus.ACTIVE);
                flowStatus = FlowStatus.UP;
            }

            dashboardLogger.onFlowStatusUpdate(flowId, flowStatus);
            flowRepository.updateStatus(flowId, flowStatus);
            stateMachine.setFlowStatus(flowStatus);

            stateMachine.saveActionToHistory(format("The flow status was set to %s", flowStatus));
        });
    }
}
