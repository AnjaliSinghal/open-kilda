/* Copyright 2018 Telstra Open Source
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

package org.openkilda.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;
import java.util.Objects;

@ToString
public class FeatureToggles implements CompositeDataEntity<FeatureToggles.FeatureTogglesData> {
    public static final FeatureToggles DEFAULTS = FeatureToggles.builder()
            .flowsRerouteOnIslDiscoveryEnabled(false)
            .createFlowEnabled(false)
            .updateFlowEnabled(false)
            .deleteFlowEnabled(false)
            .pushFlowEnabled(false)
            .unpushFlowEnabled(false)
            .useBfdForIslIntegrityCheck(true)
            .floodlightRoutePeriodicSync(true)
            .flowsRerouteViaFlowHs(false)
            .flowsRerouteUsingDefaultEncapType(false)
            .collectGrpcStats(false)
            .build();

    @Getter
    @Setter
    @Delegate
    @JsonIgnore
    private FeatureTogglesData data;

    /**
     * No args constructor for deserialization purpose.
     */
    private FeatureToggles() {
        data = new FeatureTogglesDataImpl();
    }

    public FeatureToggles(@NonNull FeatureToggles entityToClone) {
        data = FeatureTogglesCloner.INSTANCE.copy(entityToClone.getData());
    }

    @Builder
    public FeatureToggles(Boolean flowsRerouteOnIslDiscoveryEnabled, Boolean createFlowEnabled,
                          Boolean updateFlowEnabled, Boolean deleteFlowEnabled, Boolean pushFlowEnabled,
                          Boolean unpushFlowEnabled, Boolean useBfdForIslIntegrityCheck,
                          Boolean floodlightRoutePeriodicSync, Boolean flowsRerouteViaFlowHs,
                          Boolean flowsRerouteUsingDefaultEncapType, Boolean collectGrpcStats) {
        data = FeatureTogglesDataImpl.builder()
                .flowsRerouteOnIslDiscoveryEnabled(flowsRerouteOnIslDiscoveryEnabled)
                .createFlowEnabled(createFlowEnabled).updateFlowEnabled(updateFlowEnabled)
                .deleteFlowEnabled(deleteFlowEnabled).pushFlowEnabled(pushFlowEnabled)
                .unpushFlowEnabled(unpushFlowEnabled).useBfdForIslIntegrityCheck(useBfdForIslIntegrityCheck)
                .floodlightRoutePeriodicSync(floodlightRoutePeriodicSync).flowsRerouteViaFlowHs(flowsRerouteViaFlowHs)
                .flowsRerouteUsingDefaultEncapType(flowsRerouteUsingDefaultEncapType)
                .collectGrpcStats(collectGrpcStats)
                .build();
    }

    public FeatureToggles(@NonNull FeatureTogglesData data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureToggles that = (FeatureToggles) o;
        return new EqualsBuilder()
                .append(getFlowsRerouteOnIslDiscoveryEnabled(), that.getFlowsRerouteOnIslDiscoveryEnabled())
                .append(getCreateFlowEnabled(), that.getCreateFlowEnabled())
                .append(getUpdateFlowEnabled(), that.getUpdateFlowEnabled())
                .append(getDeleteFlowEnabled(), that.getDeleteFlowEnabled())
                .append(getPushFlowEnabled(), that.getPushFlowEnabled())
                .append(getUnpushFlowEnabled(), that.getUnpushFlowEnabled())
                .append(getUseBfdForIslIntegrityCheck(), that.getUseBfdForIslIntegrityCheck())
                .append(getFloodlightRoutePeriodicSync(), that.getFloodlightRoutePeriodicSync())
                .append(getFlowsRerouteViaFlowHs(), that.getFlowsRerouteViaFlowHs())
                .append(getFlowsRerouteUsingDefaultEncapType(), that.getFlowsRerouteUsingDefaultEncapType())
                .append(getCollectGrpcStats(), that.getCollectGrpcStats())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFlowsRerouteOnIslDiscoveryEnabled(), getCreateFlowEnabled(),
                getUpdateFlowEnabled(), getDeleteFlowEnabled(), getPushFlowEnabled(), getUnpushFlowEnabled(),
                getUseBfdForIslIntegrityCheck(), getFloodlightRoutePeriodicSync(), getFlowsRerouteViaFlowHs(),
                getFlowsRerouteUsingDefaultEncapType(), getCollectGrpcStats());
    }

    /**
     * Defines persistable data of the FeatureToggles.
     */
    public interface FeatureTogglesData {
        Boolean getFlowsRerouteOnIslDiscoveryEnabled();

        void setFlowsRerouteOnIslDiscoveryEnabled(Boolean flowsRerouteOnIslDiscoveryEnabled);

        Boolean getCreateFlowEnabled();

        void setCreateFlowEnabled(Boolean createFlowEnabled);

        Boolean getUpdateFlowEnabled();

        void setUpdateFlowEnabled(Boolean updateFlowEnabled);

        Boolean getDeleteFlowEnabled();

        void setDeleteFlowEnabled(Boolean deleteFlowEnabled);

        Boolean getPushFlowEnabled();

        void setPushFlowEnabled(Boolean pushFlowEnabled);

        Boolean getUnpushFlowEnabled();

        void setUnpushFlowEnabled(Boolean unpushFlowEnabled);

        Boolean getUseBfdForIslIntegrityCheck();

        void setUseBfdForIslIntegrityCheck(Boolean useBfdForIslIntegrityCheck);

        Boolean getFloodlightRoutePeriodicSync();

        void setFloodlightRoutePeriodicSync(Boolean floodlightRoutePeriodicSync);

        Boolean getFlowsRerouteViaFlowHs();

        void setFlowsRerouteViaFlowHs(Boolean flowsRerouteViaFlowHs);

        Boolean getFlowsRerouteUsingDefaultEncapType();

        void setFlowsRerouteUsingDefaultEncapType(Boolean flowsRerouteUsingDefaultEncapType);

        Boolean getCollectGrpcStats();

        void setCollectGrpcStats(Boolean collectGrpcStats);
    }

    /**
     * POJO implementation of FeatureTogglesData.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static final class FeatureTogglesDataImpl implements FeatureTogglesData, Serializable {
        private static final long serialVersionUID = 1L;
        Boolean flowsRerouteOnIslDiscoveryEnabled;
        Boolean createFlowEnabled;
        Boolean updateFlowEnabled;
        Boolean deleteFlowEnabled;
        Boolean pushFlowEnabled;
        Boolean unpushFlowEnabled;
        Boolean useBfdForIslIntegrityCheck;
        Boolean floodlightRoutePeriodicSync;
        Boolean flowsRerouteViaFlowHs;
        Boolean flowsRerouteUsingDefaultEncapType;
        Boolean collectGrpcStats;
    }

    /**
     * A cloner for FeatureToggles entity.
     */
    @Mapper
    public interface FeatureTogglesCloner {
        FeatureTogglesCloner INSTANCE = Mappers.getMapper(FeatureTogglesCloner.class);

        void copy(FeatureTogglesData source, @MappingTarget FeatureTogglesData target);

        /**
         * Performs deep-copy of entity data.
         */
        default FeatureTogglesData copy(FeatureTogglesData source) {
            FeatureTogglesData result = new FeatureTogglesDataImpl();
            copy(source, result);
            return result;
        }
    }
}
