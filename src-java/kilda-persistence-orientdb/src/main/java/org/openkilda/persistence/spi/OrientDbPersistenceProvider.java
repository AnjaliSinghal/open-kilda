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

package org.openkilda.persistence.spi;

import org.openkilda.config.provider.ConfigurationProvider;
import org.openkilda.persistence.Neo4jConfig;
import org.openkilda.persistence.Neo4jPersistenceManager;
import org.openkilda.persistence.NetworkConfig;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.persistence.orientdb.OrientDbConfig;
import org.openkilda.persistence.orientdb.OrientDbPersistenceManager;

/**
 * OrientDb implementation of the service provider for persistence manager(s).
 */
public class OrientDbPersistenceProvider implements PersistenceProvider {
    @Override
    public PersistenceManager createPersistenceManager(ConfigurationProvider configurationProvider) {
        OrientDbConfig orientDbConfig = configurationProvider.getConfiguration(OrientDbConfig.class);
        NetworkConfig networkConfig = configurationProvider.getConfiguration(NetworkConfig.class);
        return new OrientDbPersistenceManager(orientDbConfig, networkConfig);
    }
}
