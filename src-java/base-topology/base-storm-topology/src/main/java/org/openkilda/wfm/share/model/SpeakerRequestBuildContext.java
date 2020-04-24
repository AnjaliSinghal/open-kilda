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

package org.openkilda.wfm.share.model;

import org.openkilda.model.MacAddress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class SpeakerRequestBuildContext {
    public static final SpeakerRequestBuildContext EMPTY = SpeakerRequestBuildContext.builder()
            .forward(PathContext.builder().build())
            .reverse(PathContext.builder().build())
            .build();

    private PathContext forward;
    private PathContext reverse;


    /**
     * Swap forward and reverse PathContext.
     */
    public void swap() {
        PathContext temporary = forward;
        forward = reverse;
        reverse = temporary;
    }

    @Data
    @Builder
    public static class PathContext {
        private boolean removeCustomerPortRule;
        private boolean removeCustomerPortLldpRule;
        private boolean removeCustomerPortArpRule;
        private boolean removeServer42InputRule;
        private boolean installServer42InputRule;
        private Integer server42Port;
        private MacAddress server42MacAddress;
    }
}
