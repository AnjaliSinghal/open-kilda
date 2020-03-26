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

package org.openkilda.wfm.topology.flowhs.fsm.common;

import org.openkilda.floodlight.api.request.factory.FlowSegmentRequestFactory;
import org.openkilda.floodlight.api.response.SpeakerFlowSegmentResponse;
import org.openkilda.floodlight.flow.response.FlowErrorResponse;
import org.openkilda.model.FlowPathStatus;
import org.openkilda.wfm.CommandContext;
import org.openkilda.wfm.share.flow.resources.FlowResources;
import org.openkilda.wfm.share.model.FlowPathSpeakerView;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Slf4j
public abstract class FlowPathSwappingFsm<T extends NbTrackableFsm<T, S, E, C>, S, E, C>
        extends NbTrackableFsm<T, S, E, C> {

    protected final String flowId;

    // TODO - ensure usage
    protected FlowResources newPrimaryResources;
    protected FlowResources newProtectedResources;

    protected FlowPathSpeakerView newPrimaryForwardPath;
    protected FlowPathSpeakerView newPrimaryReversePath;
    protected FlowPathSpeakerView newProtectedForwardPath;
    protected FlowPathSpeakerView newProtectedReversePath;

    protected final Collection<FlowResources> oldResources = new ArrayList<>();
    protected FlowPathSpeakerView oldPrimaryForwardPath;
    protected FlowPathStatus oldPrimaryForwardPathStatus;
    protected FlowPathSpeakerView oldPrimaryReversePath;
    protected FlowPathStatus oldPrimaryReversePathStatus;
    protected FlowPathSpeakerView oldProtectedForwardPath;
    protected FlowPathStatus oldProtectedForwardPathStatus;
    protected FlowPathSpeakerView oldProtectedReversePath;
    protected FlowPathStatus oldProtectedReversePathStatus;

    protected final Set<UUID> pendingCommands = new HashSet<>();
    protected final Map<UUID, Integer> retriedCommands = new HashMap<>();
    protected final Map<UUID, FlowErrorResponse> failedCommands = new HashMap<>();
    protected final Map<UUID, SpeakerFlowSegmentResponse> failedValidationResponses = new HashMap<>();

    protected final Map<UUID, FlowSegmentRequestFactory> ingressCommands = new HashMap<>();
    protected final Map<UUID, FlowSegmentRequestFactory> nonIngressCommands = new HashMap<>();
    protected final Map<UUID, FlowSegmentRequestFactory> removeCommands = new HashMap<>();

    protected String errorReason;

    public FlowPathSwappingFsm(CommandContext commandContext, String flowId) {
        super(commandContext);
        this.flowId = flowId;
    }

    public FlowSegmentRequestFactory getInstallCommand(UUID commandId) {
        FlowSegmentRequestFactory requestFactory = nonIngressCommands.get(commandId);
        if (requestFactory == null) {
            requestFactory = ingressCommands.get(commandId);
        }
        return requestFactory;
    }

    public abstract void fireNoPathFound(String errorReason);
}
