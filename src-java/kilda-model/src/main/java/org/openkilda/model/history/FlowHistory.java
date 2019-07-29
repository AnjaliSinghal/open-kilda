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

package org.openkilda.model.history;

import org.openkilda.model.CompositeDataEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents information about the flow history.
 * The history log always related to particular Flow Event and represents inside Kilda actions caused by the Flow Event.
 */
public class FlowHistory implements CompositeDataEntity<FlowHistory.FlowHistoryData> {
    @Getter
    @Delegate
    @JsonIgnore
    private FlowHistoryData data;

    public FlowHistory() {
        data = new FlowHistoryDataImpl();
    }

    public FlowHistory(@NonNull FlowHistoryData data) {
        this.data = data;
    }

    /**
     * Defines persistable data of the FlowHistory.
     */
    public interface FlowHistoryData {
        Instant getTimestamp();

        void setTimestamp(Instant timestamp);

        String getAction();

        void setAction(String action);

        String getTaskId();

        void setTaskId(String taskId);

        String getDetails();

        void setDetails(String details);

        FlowEvent getFlowEvent();

        void setFlowEvent(FlowEvent flowEvent);
    }

    /**
     * POJO implementation of FlowHistoryData entity.
     */
    @Data
    @NoArgsConstructor
    final class FlowHistoryDataImpl implements FlowHistoryData, Serializable {
        private static final long serialVersionUID = 1L;
        Instant timestamp;
        String action;
        String taskId;
        String details;
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        FlowEvent flowEvent;
    }

    @Mapper
    public interface FlowHistoryCloner {
        FlowHistoryCloner INSTANCE = Mappers.getMapper(FlowHistoryCloner.class);

        void copy(FlowHistoryData source, @MappingTarget FlowHistoryData target);
    }
}
