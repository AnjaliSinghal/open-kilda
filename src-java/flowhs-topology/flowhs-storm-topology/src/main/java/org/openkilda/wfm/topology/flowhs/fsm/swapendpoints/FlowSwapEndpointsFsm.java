/* Copyright 2020 Telstra Open Source
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

package org.openkilda.wfm.topology.flowhs.fsm.swapendpoints;

import org.openkilda.messaging.Message;
import org.openkilda.messaging.command.flow.FlowRequest;
import org.openkilda.messaging.error.ErrorData;
import org.openkilda.messaging.error.ErrorType;
import org.openkilda.messaging.info.flow.FlowResponse;
import org.openkilda.model.Flow;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.wfm.CommandContext;
import org.openkilda.wfm.topology.flowhs.fsm.common.NbTrackableFsm;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.FlowSwapEndpointsFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.FlowSwapEndpointsFsm.State;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.OnFinishedAction;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.OnFinishedWithErrorAction;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.OnReceivedUpdateResponseAction;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.ReceivingUpdateResponsesCompletedAction;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.RevertUpdateRequestAction;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.UpdateRequestAction;
import org.openkilda.wfm.topology.flowhs.fsm.swapendpoints.actions.ValidateFlowsAction;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlow;
import org.openkilda.wfm.topology.flowhs.service.FlowSwapEndpointsHubCarrier;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class FlowSwapEndpointsFsm extends NbTrackableFsm<FlowSwapEndpointsFsm, State, Event, FlowSwapEndpointsContext> {
    public static final String ERROR_DESCRIPTION = "Could not swap endpoints";
    public static final int REQUEST_COUNT = 2;

    private final FlowSwapEndpointsHubCarrier carrier;

    private RequestedFlow firstTargetFlow;
    private RequestedFlow secondTargetFlow;
    private String firstFlowId;
    private String secondFlowId;

    private int awaitingResponses = REQUEST_COUNT;
    private List<FlowResponse> flowResponses = new ArrayList<>();
    private Flow firstOriginalFlow;
    private Flow secondOriginalFlow;

    public FlowSwapEndpointsFsm(CommandContext commandContext, FlowSwapEndpointsHubCarrier carrier,
                                RequestedFlow firstTargetFlow, RequestedFlow secondTargetFlow) {
        super(commandContext);
        this.carrier = carrier;
        this.firstTargetFlow = firstTargetFlow;
        this.secondTargetFlow = secondTargetFlow;
        this.firstFlowId = firstTargetFlow.getFlowId();
        this.secondFlowId = secondTargetFlow.getFlowId();
    }

    @Override
    public String getFlowId() {
        throw new UnsupportedOperationException("Not implemented for swap flow endpoints operation. Skipping");
    }

    @Override
    public void fireNext(FlowSwapEndpointsContext context) {
        fire(Event.NEXT, context);
    }

    @Override
    public void fireError(String errorReason) {
        fireError(new ErrorData(ErrorType.DATA_INVALID, errorReason, ERROR_DESCRIPTION));
    }

    /**
     * Fire ERROR with ErrorData.
     */
    public void fireError(ErrorData errorData) {
        log.error("Flow swap endpoints failed: {}", errorData.getErrorMessage());
        FlowSwapEndpointsContext context = new FlowSwapEndpointsContext(errorData);
        fire(Event.ERROR, context);
    }

    /**
     * Fire NEXT if all responses received.
     */
    public void fireNextWhenResponsesReceived() {
        if (--awaitingResponses == 0) {
            fire(Event.NEXT);
        }
    }

    @Override
    public void sendNorthboundResponse(Message message) {
        carrier.sendNorthboundResponse(message);
    }

    public void sendFlowUpdateRequest(FlowRequest flowRequest) {
        carrier.sendFlowUpdateRequest(flowRequest);
    }

    public static class Factory {
        private final StateMachineBuilder<FlowSwapEndpointsFsm, State, Event, FlowSwapEndpointsContext> builder;
        private final FlowSwapEndpointsHubCarrier carrier;

        public Factory(FlowSwapEndpointsHubCarrier carrier, PersistenceManager persistenceManager) {
            this.carrier = carrier;

            builder = StateMachineBuilderFactory.create(FlowSwapEndpointsFsm.class, State.class, Event.class,
                    FlowSwapEndpointsContext.class, CommandContext.class, FlowSwapEndpointsHubCarrier.class,
                    RequestedFlow.class, RequestedFlow.class);

            builder.externalTransition().from(State.INITIALIZED).to(State.FLOWS_VALIDATED).on(Event.NEXT)
                    .perform(new ValidateFlowsAction(persistenceManager));

            builder.externalTransition().from(State.FLOWS_VALIDATED).to(State.FINISHED_WITH_ERROR).on(Event.ERROR);
            builder.externalTransition().from(State.FLOWS_VALIDATED).to(State.SEND_UPDATE_REQUESTS).on(Event.NEXT)
                    .perform(new UpdateRequestAction());

            builder.internalTransition().within(State.SEND_UPDATE_REQUESTS).on(Event.RESPONSE_RECEIVED)
                    .perform(new OnReceivedUpdateResponseAction());
            builder.internalTransition().within(State.SEND_UPDATE_REQUESTS).on(Event.ERROR)
                    .perform(new OnReceivedUpdateResponseAction());
            builder.externalTransition()
                    .from(State.SEND_UPDATE_REQUESTS).to(State.FINISHED_WITH_ERROR).on(Event.TIMEOUT);
            builder.externalTransition()
                    .from(State.SEND_UPDATE_REQUESTS).to(State.RECEIVING_RESPONSES_COMPLETED).on(Event.NEXT)
                    .perform(new ReceivingUpdateResponsesCompletedAction());

            builder.externalTransition().from(State.RECEIVING_RESPONSES_COMPLETED).to(State.FINISHED).on(Event.NEXT);
            builder.externalTransition()
                    .from(State.RECEIVING_RESPONSES_COMPLETED).to(State.SEND_REVERT_UPDATE_REQUESTS).on(Event.REVERT)
                    .perform(new RevertUpdateRequestAction());

            builder.internalTransition().within(State.SEND_REVERT_UPDATE_REQUESTS).on(Event.RESPONSE_RECEIVED)
                    .perform(new OnReceivedUpdateResponseAction());
            builder.internalTransition().within(State.SEND_REVERT_UPDATE_REQUESTS).on(Event.ERROR)
                    .perform(new OnReceivedUpdateResponseAction());
            builder.externalTransition()
                    .from(State.SEND_REVERT_UPDATE_REQUESTS).to(State.FINISHED_WITH_ERROR).on(Event.TIMEOUT);
            builder.externalTransition()
                    .from(State.SEND_REVERT_UPDATE_REQUESTS).to(State.FINISHED_WITH_ERROR).on(Event.NEXT);

            builder.defineFinalState(State.FINISHED)
                    .addEntryAction(new OnFinishedAction(persistenceManager));
            builder.defineFinalState(State.FINISHED_WITH_ERROR)
                    .addEntryAction(new OnFinishedWithErrorAction(persistenceManager));
        }

        public FlowSwapEndpointsFsm newInstance(CommandContext commandContext,
                                                RequestedFlow firstTargetFlow, RequestedFlow secondTargetFlow) {
            return builder.newStateMachine(FlowSwapEndpointsFsm.State.INITIALIZED, commandContext, carrier,
                    firstTargetFlow, secondTargetFlow);
        }
    }

    public enum State {
        INITIALIZED,
        FLOWS_VALIDATED,
        SEND_UPDATE_REQUESTS,
        RECEIVING_RESPONSES_COMPLETED,
        SEND_REVERT_UPDATE_REQUESTS,
        FINISHED_WITH_ERROR,
        FINISHED
    }

    public enum Event {
        NEXT,
        RESPONSE_RECEIVED,
        REVERT,
        TIMEOUT,
        ERROR
    }
}
