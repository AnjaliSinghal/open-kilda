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

package org.openkilda.wfm.topology.flowhs.fsm.update.actions;

import static java.lang.String.format;

import org.openkilda.floodlight.flow.request.InstallIngressRule;
import org.openkilda.floodlight.flow.response.FlowResponse;
import org.openkilda.floodlight.flow.response.FlowRuleResponse;
import org.openkilda.model.Switch;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.persistence.repositories.SwitchRepository;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.HistoryRecordingAction;
import org.openkilda.wfm.topology.flowhs.fsm.update.FlowUpdateContext;
import org.openkilda.wfm.topology.flowhs.fsm.update.FlowUpdateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.update.FlowUpdateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.update.FlowUpdateFsm.State;
import org.openkilda.wfm.topology.flowhs.validation.rules.IngressRulesValidator;
import org.openkilda.wfm.topology.flowhs.validation.rules.RulesValidator;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ValidateIngressRulesAction extends
        HistoryRecordingAction<FlowUpdateFsm, State, Event, FlowUpdateContext> {
    private final SwitchRepository switchRepository;

    public ValidateIngressRulesAction(PersistenceManager persistenceManager) {
        this.switchRepository = persistenceManager.getRepositoryFactory().createSwitchRepository();
    }

    @Override
    protected void perform(State from, State to, Event event, FlowUpdateContext context, FlowUpdateFsm stateMachine) {
        FlowResponse response = context.getSpeakerFlowResponse();
        UUID commandId = response.getCommandId();
        stateMachine.getPendingCommands().remove(commandId);

        InstallIngressRule expected = stateMachine.getIngressCommands().get(commandId);
        if (expected == null) {
            throw new IllegalStateException(format("Failed to find ingress command with id %s", commandId));
        }

        if (response.isSuccess()) {
            Switch switchObj = switchRepository.findById(expected.getSwitchId())
                    .orElseThrow(() -> new IllegalStateException(format("Failed to find switch %s",
                            expected.getSwitchId())));

            RulesValidator validator = new IngressRulesValidator(expected, (FlowRuleResponse) response,
                    switchObj.getFeatures());
            if (validator.validate()) {
                stateMachine.saveActionToHistory("Rule was validated",
                        format("The ingress rule has been validated successfully: switch %s, cookie %s",
                                expected.getSwitchId(), expected.getCookie()));
            } else {
                stateMachine.saveErrorToHistory("Rule is missing or invalid",
                        format("The ingress rule is missing or invalid: switch %s, cookie %s",
                                expected.getSwitchId(), expected.getCookie()));

                stateMachine.getFailedValidationResponses().put(commandId, response);
            }
        } else {
            stateMachine.saveErrorToHistory("Rule validation failed",
                    format("Failed to validate the ingress rule: switch %s, cookie %s",
                            expected.getSwitchId(), expected.getCookie()));

            stateMachine.getFailedValidationResponses().put(commandId, response);
        }

        if (stateMachine.getPendingCommands().isEmpty()) {
            if (stateMachine.getFailedValidationResponses().isEmpty()) {
                log.debug("Ingress rules have been validated for flow {}", stateMachine.getFlowId());
                stateMachine.fire(Event.RULES_VALIDATED);
            } else {
                stateMachine.saveErrorToHistory(
                        "Found missing rules or received error response(s) on validation commands");
                stateMachine.fire(Event.MISSING_RULE_FOUND);
            }
        }
    }
}
