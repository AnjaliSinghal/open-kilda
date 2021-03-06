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

package org.openkilda.persistence.repositories.impl;

import static java.lang.String.format;

import org.openkilda.model.FlowMeter;
import org.openkilda.model.MeterId;
import org.openkilda.model.PathId;
import org.openkilda.model.SwitchId;
import org.openkilda.persistence.PersistenceException;
import org.openkilda.persistence.TransactionManager;
import org.openkilda.persistence.repositories.FlowMeterRepository;

import com.google.common.collect.ImmutableMap;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Neo4j OGM implementation of {@link FlowMeterRepository}.
 */
public class Neo4jFlowMeterRepository extends Neo4jGenericRepository<FlowMeter> implements FlowMeterRepository {
    static final String PATH_ID_PROPERTY_NAME = "path_id";

    public Neo4jFlowMeterRepository(Neo4jSessionFactory sessionFactory, TransactionManager transactionManager) {
        super(sessionFactory, transactionManager);
    }

    @Override
    public Optional<FlowMeter> findByPathId(PathId pathId) {
        Filter pathIdFilter = new Filter(PATH_ID_PROPERTY_NAME, ComparisonOperator.EQUALS, pathId);

        Collection<FlowMeter> meters = loadAll(pathIdFilter);
        if (meters.size() > 1) {
            throw new PersistenceException(format("Found more that 1 Meter entity by path (%s)", pathId));
        }
        return meters.isEmpty() ? Optional.empty() : Optional.of(meters.iterator().next());
    }

    @Override
    public Optional<MeterId> findUnassignedMeterId(SwitchId switchId, MeterId minMeterId, MeterId maxMeterId) {
        Map<String, Object> parameters = ImmutableMap.of(
                "min_meter", minMeterId.getValue(),
                "max_meter", maxMeterId.getValue(),
                "switch_id", switchId.toString()
        );

        // The query returns the min_meter if it's not used in any flow_meter,
        // otherwise locates a gap between / after the values used in flow_meter entities.

        String query = "UNWIND [$min_meter] AS meter "
                + "OPTIONAL MATCH (n:flow_meter {switch_id: $switch_id}) "
                + "WHERE meter = n.meter_id "
                + "WITH meter, n "
                + "WHERE n IS NULL "
                + "RETURN meter "
                + "UNION ALL "
                + "MATCH (n1:flow_meter {switch_id: $switch_id}) "
                + "WHERE n1.meter_id >= $min_meter AND n1.meter_id < $max_meter "
                + "OPTIONAL MATCH (n2:flow_meter {switch_id: $switch_id}) "
                + "WHERE (n1.meter_id + 1) = n2.meter_id "
                + "WITH n1, n2 "
                + "WHERE n2 IS NULL "
                + "RETURN n1.meter_id + 1 AS meter "
                + "ORDER BY meter "
                + "LIMIT 1";

        return queryForLong(query, parameters, "meter").map(MeterId::new);
    }

    @Override
    protected Class<FlowMeter> getEntityType() {
        return FlowMeter.class;
    }
}
