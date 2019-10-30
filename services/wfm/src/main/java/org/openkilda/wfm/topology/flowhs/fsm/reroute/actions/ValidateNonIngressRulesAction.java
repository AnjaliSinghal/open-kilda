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

import static java.lang.String.format;

import org.openkilda.floodlight.flow.request.InstallTransitRule;
import org.openkilda.floodlight.flow.response.FlowResponse;
import org.openkilda.floodlight.flow.response.FlowRuleResponse;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.HistoryRecordingAction;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteContext;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteFsm;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.reroute.FlowRerouteFsm.State;
import org.openkilda.wfm.topology.flowhs.validation.rules.NonIngressRulesValidator;
import org.openkilda.wfm.topology.flowhs.validation.rules.RulesValidator;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ValidateNonIngressRulesAction extends
        HistoryRecordingAction<FlowRerouteFsm, State, Event, FlowRerouteContext> {
    @Override
    protected void perform(State from, State to, Event event, FlowRerouteContext context, FlowRerouteFsm stateMachine) {
        FlowResponse response = context.getSpeakerFlowResponse();
        UUID commandId = response.getCommandId();
        stateMachine.getPendingCommands().remove(commandId);

        InstallTransitRule expected = stateMachine.getNonIngressCommands().get(commandId);
        if (expected == null) {
            throw new IllegalStateException(format("Failed to find non ingress command with id %s", commandId));
        }

        if (response.isSuccess()) {
            RulesValidator validator = new NonIngressRulesValidator(expected, (FlowRuleResponse) response);
            if (validator.validate()) {
                stateMachine.saveActionToHistory("Rule was validated",
                        format("The non ingress rule has been validated successfully: switch %s, cookie %s",
                                expected.getSwitchId(), expected.getCookie()));
            } else {
                stateMachine.saveErrorToHistory("Rule is missing or invalid",
                        format("Non ingress rule is missing or invalid: switch %s, cookie %s",
                                expected.getSwitchId(), expected.getCookie()));

                stateMachine.getFailedValidationResponses().put(commandId, response);
            }
        } else {
            stateMachine.saveErrorToHistory("Rule validation failed",
                    format("Failed to validate non ingress rule %s on switch %s",
                            expected.getCookie(), expected.getSwitchId()));

            stateMachine.getFailedValidationResponses().put(commandId, response);
        }

        if (stateMachine.getPendingCommands().isEmpty()) {
            if (stateMachine.getFailedValidationResponses().isEmpty()) {
                log.debug("Non ingress rules have been validated for flow {}", stateMachine.getFlowId());
                stateMachine.fire(Event.RULES_VALIDATED);
            } else {
                stateMachine.saveErrorToHistory(
                        "Found missing rules or received error response(s) on validation commands");
                stateMachine.fire(Event.MISSING_RULE_FOUND);
            }
        }
    }
}
