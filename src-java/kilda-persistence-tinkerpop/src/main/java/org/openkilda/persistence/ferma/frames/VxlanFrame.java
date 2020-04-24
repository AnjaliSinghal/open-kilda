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

import org.openkilda.model.PathId;
import org.openkilda.model.Vxlan.VxlanData;
import org.openkilda.persistence.ferma.frames.converters.Convert;
import org.openkilda.persistence.ferma.frames.converters.PathIdConverter;

import com.syncleus.ferma.annotations.Property;
import lombok.NonNull;

public abstract class VxlanFrame extends KildaBaseVertexFrame implements VxlanData {
    public static final String FRAME_LABEL = "transit_vlan";
    public static final String PATH_ID_PROPERTY = "path_id";
    public static final String VNI_PROPERTY = "vni";

    @Override
    @Property("flow_id")
    public abstract String getFlowId();

    @Override
    @Property("flow_id")
    public abstract void setFlowId(String flowId);

    @Override
    @Property("path_id")
    @Convert(PathIdConverter.class)
    public abstract PathId getPathId();

    @Override
    @Property("path_id")
    @Convert(PathIdConverter.class)
    public abstract void setPathId(@NonNull PathId pathId);

    @Property(VNI_PROPERTY)
    public abstract int getVni();

    @Property(VNI_PROPERTY)
    public abstract void setVni(int vni);
}
