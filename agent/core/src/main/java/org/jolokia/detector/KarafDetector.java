package org.jolokia.detector;

/*
 * Copyright 2016 Tadayoshi Sato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;

import org.jolokia.backend.executor.MBeanServerExecutor;

/**
 * Detector for Apache Karaf OSGi container
 * 
 * @author Tadayoshi Sato
 */
public class KarafDetector extends AbstractServerDetector {

    /**
     * {@inheritDoc}
     */
    public ServerHandle detect(MBeanServerExecutor pMBeanServerExecutor) {
        String version = getSingleStringAttribute(pMBeanServerExecutor, "org.apache.karaf:type=system,name=*", "Version");
        if (version == null) {
            return null;
        }
        return new ServerHandle("Apache", "karaf", version, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMBeanServers(Set<MBeanServerConnection> pServers) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    }
}
