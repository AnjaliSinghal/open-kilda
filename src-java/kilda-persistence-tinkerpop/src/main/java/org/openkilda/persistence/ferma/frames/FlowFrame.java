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

package org.openkilda.persistence.ferma.frames;

import org.openkilda.model.DetectConnectedDevices;
import org.openkilda.model.Flow.FlowData;
import org.openkilda.model.FlowEncapsulationType;
import org.openkilda.model.FlowPath;
import org.openkilda.model.FlowStatus;
import org.openkilda.model.PathComputationStrategy;
import org.openkilda.model.PathId;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ferma.frames.converters.Convert;
import org.openkilda.persistence.ferma.frames.converters.FlowEncapsulationTypeConverter;
import org.openkilda.persistence.ferma.frames.converters.FlowStatusConverter;
import org.openkilda.persistence.ferma.frames.converters.PathComputationStrategyConverter;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Property;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class FlowFrame extends KildaBaseVertexFrame implements FlowData {
    public static final String FRAME_LABEL = "flow";
    public static final String SOURCE_EDGE = "source";
    public static final String DESTINATION_EDGE = "destination";
    public static final String OWNS_PATHS_EDGE = "owns";
    public static final String FLOW_ID_PROPERTY = "flow_id";
    public static final String SRC_PORT_PROPERTY = "src_port";
    public static final String DST_PORT_PROPERTY = "dst_port";
    public static final String SRC_VLAN_PROPERTY = "src_vlan";
    public static final String DST_VLAN_PROPERTY = "dst_vlan";
    public static final String GROUP_ID_PROPERTY = "group_id";
    public static final String BANDWIDTH_PROPERTY = "bandwidth";
    public static final String PERIODIC_PINGS_PROPERTY = "periodic_pings";
    public static final String STATUS_PROPERTY = "status";
    public static final String SRC_MULTI_TABLE_PROPERTY = "src_with_multi_table";
    public static final String DST_MULTI_TABLE_PROPERTY = "dst_with_multi_table";
    public static final String SRC_LLDP_PROPERTY = "detect_src_lldp_connected_devices";
    public static final String DST_LLDP_PROPERTY = "detect_dst_lldp_connected_devices";
    public static final String SRC_ARP_PROPERTY = "detect_src_arp_connected_devices";
    public static final String DST_ARP_PROPERTY = "detect_dst_arp_connected_devices";

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract String getFlowId();

    @Override
    @Property(FLOW_ID_PROPERTY)
    public abstract void setFlowId(String flowId);

    @Override
    @Property(SRC_PORT_PROPERTY)
    public abstract int getSrcPort();

    @Override
    @Property(SRC_PORT_PROPERTY)
    public abstract void setSrcPort(int srcPort);

    @Override
    @Property(SRC_VLAN_PROPERTY)
    public abstract int getSrcVlan();

    @Override
    @Property(SRC_VLAN_PROPERTY)
    public abstract void setSrcVlan(int srcVlan);

    @Override
    @Property(DST_PORT_PROPERTY)
    public abstract int getDestPort();

    @Override
    @Property(DST_PORT_PROPERTY)
    public abstract void setDestPort(int destPort);

    @Override
    @Property(DST_VLAN_PROPERTY)
    public abstract int getDestVlan();

    @Override
    @Property(DST_VLAN_PROPERTY)
    public abstract void setDestVlan(int destVlan);

    @Override
    @Property("forward_path_id")
    @Convert(PathIdConverter.class)
    public abstract PathId getForwardPathId();

    @Override
    @Property("forward_path_id")
    @Convert(PathIdConverter.class)
    public abstract void setForwardPathId(PathId forwardPathId);

    @Override
    @Property("reverse_path_id")
    @Convert(PathIdConverter.class)
    public abstract PathId getReversePathId();

    @Override
    @Property("reverse_path_id")
    @Convert(PathIdConverter.class)
    public abstract void setReversePathId(PathId reversePathId);

    @Override
    @Property("allocate_protected_path")
    public abstract boolean isAllocateProtectedPath();

    @Override
    @Property("allocate_protected_path")
    public abstract void setAllocateProtectedPath(boolean allocateProtectedPath);

    @Override
    @Property("protected_forward_path_id")
    @Convert(PathIdConverter.class)
    public abstract PathId getProtectedForwardPathId();

    @Override
    @Property("protected_forward_path_id")
    @Convert(PathIdConverter.class)
    public abstract void setProtectedForwardPathId(PathId protectedForwardPathId);

    @Override
    @Property("protected_reverse_path_id")
    @Convert(PathIdConverter.class)
    public abstract PathId getProtectedReversePathId();

    @Override
    @Property("protected_reverse_path_id")
    @Convert(PathIdConverter.class)
    public abstract void setProtectedReversePathId(PathId protectedReversePathId);

    @Override
    @Property(GROUP_ID_PROPERTY)
    public abstract String getGroupId();

    @Override
    @Property(GROUP_ID_PROPERTY)
    public abstract void setGroupId(String groupId);

    @Override
    @Property(BANDWIDTH_PROPERTY)
    public abstract long getBandwidth();

    @Override
    @Property(BANDWIDTH_PROPERTY)
    public abstract void setBandwidth(long bandwidth);

    @Override
    @Property("ignore_bandwidth")
    public abstract boolean isIgnoreBandwidth();

    @Override
    @Property("ignore_bandwidth")
    public abstract void setIgnoreBandwidth(boolean ignoreBandwidth);

    @Override
    @Property("description")
    public abstract String getDescription();

    @Override
    @Property("description")
    public abstract void setDescription(String description);

    @Override
    @Property(PERIODIC_PINGS_PROPERTY)
    public abstract boolean isPeriodicPings();

    @Override
    @Property(PERIODIC_PINGS_PROPERTY)
    public abstract void setPeriodicPings(boolean periodicPings);

    @Override
    @Property("encapsulation_type")
    @Convert(FlowEncapsulationTypeConverter.class)
    public abstract FlowEncapsulationType getEncapsulationType();

    @Override
    @Property("encapsulation_type")
    @Convert(FlowEncapsulationTypeConverter.class)
    public abstract void setEncapsulationType(FlowEncapsulationType encapsulationType);

    @Override
    @Property(STATUS_PROPERTY)
    @Convert(FlowStatusConverter.class)
    public abstract FlowStatus getStatus();

    @Override
    @Property(STATUS_PROPERTY)
    @Convert(FlowStatusConverter.class)
    public abstract void setStatus(FlowStatus status);

    @Override
    @Property("max_latency")
    public abstract Long getMaxLatency();

    @Override
    @Property("max_latency")
    public abstract void setMaxLatency(Long maxLatency);

    @Override
    @Property("priority")
    public abstract Integer getPriority();

    @Override
    @Property("priority")
    public abstract void setPriority(Integer priority);

    @Override
    @Property("pinned")
    public abstract boolean isPinned();

    @Override
    @Property("pinned")
    public abstract void setPinned(boolean pinned);

    @Override
    public DetectConnectedDevices getDetectConnectedDevices() {
        return DetectConnectedDevices.builder()
                .srcLldp(getProperty(SRC_LLDP_PROPERTY))
                .srcArp(getProperty(SRC_ARP_PROPERTY))
                .dstLldp(getProperty(DST_LLDP_PROPERTY))
                .dstArp(getProperty(DST_ARP_PROPERTY))
                .srcSwitchLldp(getProperty("src_lldp_switch_connected_devices"))
                .srcSwitchArp(getProperty("src_arp_switch_connected_devices"))
                .dstSwitchLldp(getProperty("dst_lldp_switch_connected_devices"))
                .dstSwitchArp(getProperty("dst_arp_switch_connected_devices"))
                .build();
    }

    @Override
    public void setDetectConnectedDevices(DetectConnectedDevices detectConnectedDevices) {
        setProperty(SRC_LLDP_PROPERTY, detectConnectedDevices.isSrcLldp());
        setProperty(SRC_ARP_PROPERTY, detectConnectedDevices.isSrcArp());
        setProperty(DST_LLDP_PROPERTY, detectConnectedDevices.isDstLldp());
        setProperty(DST_ARP_PROPERTY, detectConnectedDevices.isDstArp());
        setProperty("src_lldp_switch_connected_devices", detectConnectedDevices.isSrcSwitchLldp());
        setProperty("src_arp_switch_connected_devices", detectConnectedDevices.isSrcSwitchArp());
        setProperty("dst_lldp_switch_connected_devices", detectConnectedDevices.isDstSwitchLldp());
        setProperty("dst_arp_switch_connected_devices", detectConnectedDevices.isDstSwitchArp());
    }

    @Override
    @Property(SRC_MULTI_TABLE_PROPERTY)
    public abstract boolean isSrcWithMultiTable();

    @Override
    @Property(SRC_MULTI_TABLE_PROPERTY)
    public abstract void setSrcWithMultiTable(boolean srcWithMultiTable);

    @Override
    @Property(DST_MULTI_TABLE_PROPERTY)
    public abstract boolean isDestWithMultiTable();

    @Override
    @Property(DST_MULTI_TABLE_PROPERTY)
    public abstract void setDestWithMultiTable(boolean destWithMultiTable);

    @Override
    @Property("path_computation_strategy")
    @Convert(PathComputationStrategyConverter.class)
    public abstract PathComputationStrategy getPathComputationStrategy();

    @Override
    @Property("path_computation_strategy")
    @Convert(PathComputationStrategyConverter.class)
    public abstract void setPathComputationStrategy(PathComputationStrategy pathComputationStrategy);

    @Override
    @Property("target_path_computation_strategy")
    @Convert(PathComputationStrategyConverter.class)
    public abstract PathComputationStrategy getTargetPathComputationStrategy();

    @Override
    @Property("target_path_computation_strategy")
    @Convert(PathComputationStrategyConverter.class)
    public abstract void setTargetPathComputationStrategy(PathComputationStrategy pathComputationStrategy);

    @Override
    public Switch getSrcSwitch() {
        return Optional.ofNullable(traverse(v -> v.out(SOURCE_EDGE)
                .hasLabel(SwitchFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(SwitchFrame.class, null)).map(Switch::new).orElse(null);
    }

    @Override
    public SwitchId getSrcSwitchId() {
        return traverse(v -> v.out(SOURCE_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                .values(SwitchFrame.SWITCH_ID_PROPERTY)).getRawTraversal().tryNext()
                .map(s -> (String) s).map(SwitchIdConverter.INSTANCE::toEntityAttribute).orElse(null);
    }

    @Override
    public void setSrcSwitch(Switch srcSwitch) {
        getElement().edges(Direction.OUT, SOURCE_EDGE).forEachRemaining(Edge::remove);

        Switch.SwitchData data = srcSwitch.getData();
        if (data instanceof SwitchFrame) {
            linkOut((VertexFrame) data, SOURCE_EDGE);
        } else {
            SwitchFrame frame = SwitchFrame.load(getGraph(), data.getSwitchId()).orElseThrow(() ->
                    new IllegalArgumentException("Unable to link to non-existent switch " + srcSwitch));
            linkOut(frame, SOURCE_EDGE);
        }
    }

    @Override
    public Switch getDestSwitch() {
        return Optional.ofNullable(traverse(v -> v.out(DESTINATION_EDGE)
                .hasLabel(SwitchFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(SwitchFrame.class, null)).map(Switch::new).orElse(null);
    }

    @Override
    public SwitchId getDestSwitchId() {
        return traverse(v -> v.out(DESTINATION_EDGE).hasLabel(SwitchFrame.FRAME_LABEL)
                .values(SwitchFrame.SWITCH_ID_PROPERTY)).getRawTraversal().tryNext()
                .map(s -> (String) s).map(SwitchIdConverter.INSTANCE::toEntityAttribute).orElse(null);
    }

    @Override
    public void setDestSwitch(Switch destSwitch) {
        getElement().edges(Direction.OUT, DESTINATION_EDGE).forEachRemaining(Edge::remove);

        Switch.SwitchData data = destSwitch.getData();
        if (data instanceof SwitchFrame) {
            linkOut((VertexFrame) data, DESTINATION_EDGE);
        } else {
            SwitchFrame frame = SwitchFrame.load(getGraph(), data.getSwitchId()).orElseThrow(() ->
                    new IllegalArgumentException("Unable to link to non-existent switch " + destSwitch));
            linkOut(frame, DESTINATION_EDGE);
        }
    }

    @Override
    public Collection<FlowPath> getPaths() {
        if (getElement().edges(Direction.OUT, OWNS_PATHS_EDGE).hasNext()) {
            return traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL))
                    .toListExplicit(FlowPathFrame.class).stream().map(FlowPath::new).collect(Collectors.toList());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<PathId> getPathIds() {
        return traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL)
                .values(FlowPathFrame.PATH_ID_PROPERTY)).getRawTraversal().toStream()
                .map(s -> (String) s).map(PathIdConverter.INSTANCE::toEntityAttribute).collect(Collectors.toSet());
    }

    @Override
    public Optional<FlowPath> getPath(PathId pathId) {
        return Optional.ofNullable(
                traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL)
                        .has(FlowPathFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.toGraphProperty(pathId)))
                        .nextOrDefaultExplicit(FlowPathFrame.class, null))
                .map(FlowPath::new);
    }

    @Override
    public boolean hasPath(FlowPath path) {
        return traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL)
                .has(FlowPathFrame.PATH_ID_PROPERTY, PathIdConverter.INSTANCE.toGraphProperty(path.getPathId())))
                .getRawTraversal().tryNext().isPresent();
    }

    @Override
    public void addPaths(FlowPath... paths) {
        for (FlowPath path : paths) {
            FlowPath.FlowPathData data = path.getData();
            VertexFrame frame;
            if (data instanceof FlowPathFrame) {
                frame = (VertexFrame) data;
                // Unlink the path from the previous owner.
                frame.getElement().edges(Direction.IN, FlowFrame.OWNS_PATHS_EDGE).forEachRemaining(Edge::remove);
            } else {
                // We intentionally don't allow to add transient entities.
                // A path must be added via corresponding repository first.
                throw new IllegalArgumentException("Unable to link to transient flow path " + path);
            }
            linkOut(frame, OWNS_PATHS_EDGE);
        }
    }

    @Override
    public void removeAllPaths() {
        traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class)
                .forEach(pathFrame -> {
                    unlinkOut(pathFrame, OWNS_PATHS_EDGE);
                    pathFrame.removeWithSegments();
                });
    }

    @Override
    public void removePaths(PathId... pathIds) {
        Set<PathId> pathIdsToRemove = new HashSet<>(Arrays.asList(pathIds));

        traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class)
                .forEach(pathFrame -> {
                    if (pathIdsToRemove.contains(pathFrame.getPathId())) {
                        unlinkOut(pathFrame, OWNS_PATHS_EDGE);
                        pathFrame.removeWithSegments();
                    }
                });
    }

    public void removeWithPaths() {
        traverse(v -> v.out(OWNS_PATHS_EDGE).hasLabel(FlowPathFrame.FRAME_LABEL))
                .toListExplicit(FlowPathFrame.class)
                .forEach(FlowPathFrame::removeWithSegments);
        remove();
    }
}