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

import org.openkilda.model.Cookie;
import org.openkilda.model.Flow;
import org.openkilda.model.FlowPath;
import org.openkilda.model.FlowPath.FlowPathData;
import org.openkilda.model.FlowPathStatus;
import org.openkilda.model.MeterId;
import org.openkilda.model.PathId;
import org.openkilda.model.PathSegment;
import org.openkilda.model.Switch;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.ConstraintViolationException;
import org.openkilda.persistence.ferma.frames.converters.Convert;
import org.openkilda.persistence.ferma.frames.converters.CookieConverter;
import org.openkilda.persistence.ferma.frames.converters.FlowPathStatusConverter;
import org.openkilda.persistence.ferma.frames.converters.MeterIdConverter;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;
import org.openkilda.persistence.ferma.frames.converters.SwitchIdConverter;

import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Property;
import lombok.NonNull;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class FlowPathFrame extends KildaBaseVertexFrame implements FlowPathData {
    public static final String FRAME_LABEL = "flow_path";
    public static final String SOURCE_EDGE = "source";
    public static final String DESTINATION_EDGE = "destination";
    public static final String OWNS_SEGMENTS_EDGE = "owns";
    public static final String PATH_ID_PROPERTY = "path_id";
    public static final String COOKIE_PROPERTY = "cookie";
    public static final String IGNORE_BANDWIDTH_PROPERTY = "ignore_bandwidth";
    public static final String BANDWIDTH_PROPERTY = "bandwidth";

    @Override
    @Property(PATH_ID_PROPERTY)
    @Convert(PathIdConverter.class)
    public abstract PathId getPathId();

    @Override
    @Property(PATH_ID_PROPERTY)
    @Convert(PathIdConverter.class)
    public abstract void setPathId(@NonNull PathId pathId);

    @Override
    @Property(COOKIE_PROPERTY)
    @Convert(CookieConverter.class)
    public abstract Cookie getCookie();

    @Override
    @Property(COOKIE_PROPERTY)
    @Convert(CookieConverter.class)
    public abstract void setCookie(Cookie cookie);

    @Override
    @Property("meter_id")
    @Convert(MeterIdConverter.class)
    public abstract MeterId getMeterId();

    @Override
    @Property("meter_id")
    @Convert(MeterIdConverter.class)
    public abstract void setMeterId(MeterId meterId);

    @Override
    public long getLatency() {
        return Optional.ofNullable((Long) getProperty("latency")).orElse(0L);
    }

    @Override
    @Property("latency")
    public abstract void setLatency(long latency);

    @Override
    @Property(BANDWIDTH_PROPERTY)
    public abstract long getBandwidth();

    @Override
    @Property(BANDWIDTH_PROPERTY)
    public abstract void setBandwidth(long bandwidth);

    @Override
    @Property(IGNORE_BANDWIDTH_PROPERTY)
    public abstract boolean isIgnoreBandwidth();

    @Override
    @Property(IGNORE_BANDWIDTH_PROPERTY)
    public abstract void setIgnoreBandwidth(boolean ignoreBandwidth);

    @Override
    @Property("status")
    @Convert(FlowPathStatusConverter.class)
    public abstract FlowPathStatus getStatus();

    @Override
    @Property("status")
    @Convert(FlowPathStatusConverter.class)
    public abstract void setStatus(FlowPathStatus status);

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
    public List<PathSegment> getSegments() {
        return traverse(v -> v.out(OWNS_SEGMENTS_EDGE).hasLabel(PathSegmentFrame.FRAME_LABEL))
                .toListExplicit(PathSegmentFrame.class).stream()
                .map(PathSegment::new).sorted(Comparator.comparingInt(PathSegment::getSeqId))
                .collect(Collectors.toList());
    }

    @Override
    public void setSegments(List<PathSegment> segments) {
        getElement().edges(Direction.OUT, OWNS_SEGMENTS_EDGE).forEachRemaining(edge -> {
            edge.inVertex().remove();
            edge.remove();
        });

        for (int idx = 0; idx < segments.size(); idx++) {
            PathSegment segment = segments.get(idx);
            segment.setSeqId(idx);

            PathSegment.PathSegmentData data = segment.getData();
            VertexFrame frame;
            if (data instanceof PathSegmentFrame) {
                frame = (VertexFrame) data;
                // Unlink the path from the previous owner.
                frame.getElement().edges(Direction.IN, OWNS_SEGMENTS_EDGE).forEachRemaining(Edge::remove);
            } else {
                frame = PathSegmentFrame.create(getGraph(), data);
            }
            linkOut(frame, OWNS_SEGMENTS_EDGE);
        }
    }

    @Override
    public String getFlowId() {
        return (String) traverse(v -> v.in(FlowFrame.OWNS_PATHS_EDGE).hasLabel(FlowFrame.FRAME_LABEL)
                .values(FlowFrame.FLOW_ID_PROPERTY)).getRawTraversal().tryNext().orElse(null);
    }

    @Override
    public Flow getFlow() {
        return Optional.ofNullable(traverse(v -> v.in(FlowFrame.OWNS_PATHS_EDGE)
                .hasLabel(FlowFrame.FRAME_LABEL))
                .nextOrDefaultExplicit(FlowFrame.class, null))
                .map(Flow::new)
                .orElse(null);
    }

    public void removeWithSegments() {
        traverse(v -> v.out(OWNS_SEGMENTS_EDGE).hasLabel(PathSegmentFrame.FRAME_LABEL))
                .toListExplicit(PathSegmentFrame.class)
                .forEach(PathSegmentFrame::remove);
        remove();
    }

    public static FlowPathFrame create(FramedGraph framedGraph, FlowPathData data) {
        if ((Long) framedGraph.traverse(input -> input.V().hasLabel(FRAME_LABEL)
                .has(PATH_ID_PROPERTY, PathIdConverter.INSTANCE.toGraphProperty(data.getPathId()))
                .count()).getRawTraversal().next() > 0) {
            throw new ConstraintViolationException("Unable to create a vertex with duplicated " + PATH_ID_PROPERTY);
        }

        FlowPathFrame frame = KildaBaseVertexFrame.addNewFramedVertex(framedGraph, FRAME_LABEL, FlowPathFrame.class);
        FlowPath.FlowPathCloner.INSTANCE.copy(data, frame);
        return frame;
    }
}
