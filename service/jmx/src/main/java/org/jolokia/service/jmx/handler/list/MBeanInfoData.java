package org.jolokia.service.jmx.handler.list;

/*
 * Copyright 2009-2011 Roland Huss
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.IOException;
import java.util.*;

import javax.management.*;

import org.jolokia.json.JSONObject;
import org.jolokia.service.jmx.api.CacheKeyProvider;

/**
 * Tree of MBean metadata. This map is a container for one or more {@link MBeanInfo} metadata which can be obtained
 * via a <code>list</code> request. The full structure in its JSON representation looks like below. The amount
 * of data included can be fine-tuned in two ways:
 * <ul>
 *     <li>With a <code>maxDepth</code> parameter given at construction time, the size of the map can be restricted
 *     (from top down)</li>
 *     <li>A given path selects only partial information from the tree</li>
 * </ul>
 * Both limiting factors are taken care of when adding the information so that this map doesn't get unnecessarily
 * to large.
 *
 * <pre>{@code
 * {
 *   <domain> : {
 *     <prop list> : {
 *       "attr" : {
 *         <attr name> : {
 *           "type" : <attribute type>,
 *           "desc" : <textual description of attribute>,
 *           "rw"   : true/false
 *         },
 *         ...
 *       },
 *       "op" : {
 *         <operation name> : {
 *           "args" : [
 *             {
 *               "type" : <argument type>,
 *               "name" : <argument name>,
 *               "desc" : <textual description of argument>
 *             },
 *             ...
 *           ],
 *           "ret"  : <return type>,
 *           "desc" : <textual description of operation>
 *         },
 *         ...
 *       },
 *       "notif" : {
 *         <notification type> : {
 *           "name" : <name>,
 *           "desc" : <desc>,
 *           "types" : [ <type1>, <type2>, ... ]
 *         },
 *         ...
 *       }
 *     },
 *     ...
 *   },
 *   ...
 * }
 * }</pre>
 *
 * @author roland
 * @since 13.09.11
 */
public class MBeanInfoData {

    // max depth for map to return
    private final int maxDepth;

    // stack for an inner path
    private final Deque<String> pathStack;

    // Map holding information. Without narrowing the list (using maxDepth), this should be:
    // domain -> mbean (by key property listing) -> JSONified mbeanInfo
    // for opitmized list() variant, the map is a bit more complex and the above mapping is under "domains" key,
    // while "cache" key contains full, JSONified MBeanInfo
    private final Map<String, Object> infoMap = new JSONObject();

    // static updaters for basic mapping of javax.management.MBeanInfo
    private static final Map<String, DataUpdater> UPDATERS = new HashMap<>();
    private static final DataUpdater LIST_KEYS_UPDATER = new ListKeysDataUpdater();

    // How to order keys in Object Names
    private final boolean useCanonicalName;

    // whether to add a map of keys from Object name to MBeanInfo data of the MBean
    private final boolean listKeys;

    // whether to use optimized list() response (with cache/domain)
    private final boolean listCache;

    static {
        for (DataUpdater updater : new DataUpdater[] {
                new DescriptionDataUpdater(),
                new ClassNameDataUpdater(),
                new AttributeDataUpdater(),
                new OperationDataUpdater(),
                new NotificationDataUpdater(),
        }) {
            UPDATERS.put(updater.getKey(),updater);
        }
    }

    // Provider to prepend (if not null). Only org.jolokia.service.jsr160.Jsr160RequestHandler declares "proxy"
    // (literally) provider.
    private final String pProvider;

    /**
     * Constructor taking a max depth. The <em>max depth</em> specifies how deep the info tree should be build
     * up. The tree will be truncated if it gets larger than this value. A <em>path</em> (in form of a stack)
     * can be given, in which only a sub information (subtree or leaf value) is stored
     *
     * @param pMaxDepth         max depth
     * @param pPathStack        the stack for restricting the information to add. The given stack will be cloned
     *                          and is left untouched.
     * @param pUseCanonicalName whether to use canonical name in listings
     * @param pListKeys
     * @param pListCache
     */
    public MBeanInfoData(int pMaxDepth, Deque<String> pPathStack, boolean pUseCanonicalName, boolean pListKeys, boolean pListCache, String pProvider) {
        maxDepth = pMaxDepth;
        useCanonicalName = pUseCanonicalName;
        listKeys = pListKeys;
        listCache = pListCache;
        pathStack = pPathStack != null ? new LinkedList<>(pPathStack) : new LinkedList<>();
        this.pProvider = pProvider;
    }

    /**
     * The first two levels of this map (tree) consist of the MBean's domain name and name properties, which are
     * independent of an MBean's metadata. If the max depth given at construction time is less or equals than 2 (and
     * no inner path into the map is given), then a client of this map does not need to query the MBeanServer for
     * MBeanInfo metadata.
     * <p></p>
     * This method checks this condition and returns true if this is the case. As side effect it will update this
     * map with the name part extracted from the given object name
     *
     * @param pName the objectname used for the first two levels
     * @return true if the object name has been added.
     */
    public boolean handleFirstOrSecondLevel(ObjectName pName) {
        if (maxDepth == 1 && pathStack.isEmpty()) {
            // Only add domain names with a dummy value if max depth is restricted to 1
            // But only when used without path
            infoMap.put(addProviderIfNeeded(pName.getDomain()), 1);
            return true;
        } else if (maxDepth == 2 && pathStack.isEmpty()) {
            // Add domain an object name into the map, final value is a dummy value
            Map<String, Object> domain = getOrCreateJSONObject(infoMap, addProviderIfNeeded(pName.getDomain()));
            domain.put(getKeyPropertyString(pName),1);
            return true;
        }
        return false;
    }

    /**
     * Turn {@link ObjectName} into a String depending on {@link org.jolokia.server.core.config.ConfigKey#CANONICAL_NAMING}
     * property setting.
     * @param pName
     * @return
     */
    private String getKeyPropertyString(ObjectName pName) {
        return useCanonicalName ? pName.getCanonicalKeyPropertyListString() : pName.getKeyPropertyListString();
    }

    /**
     * Add information about an MBean as obtained from an {@link MBeanInfo} descriptor. The information added
     * can be restricted by a given path (which has already been prepared as a stack). Also, a max depth as given in the
     * constructor restricts the size of the map from the top.
     *
     * @param pConn             {@link MBeanServerConnection} to get MBeanInfo from (or from cache if possible)
     * @param pInstance         the object instance of the MBean
     * @param customUpdaters    additional set of discovered updaters to enhance the constructed MBeanInfo (JSON data)
     * @param cacheKeyProviders set of services that help to construct the cache of MBeanInfo
     */
    public void addMBeanInfo(MBeanServerConnection pConn, ObjectInstance pInstance, Set<DataUpdater> customUpdaters,
                             Set<CacheKeyProvider> cacheKeyProviders)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {

        ObjectName objectName = pInstance.getObjectName();
        MBeanInfo mBeanInfo = pConn.getMBeanInfo(objectName);
        String domainName = addProviderIfNeeded(objectName.getDomain());
        String mbeanKeyListing = getKeyPropertyString(objectName);

        // Trim down stack to get rid of domain/property list
        Deque<String> stack = truncatePathStack(2);

        Map<String, Object> cache = null;
        Map<String, Object> domains;
        Map<String, Object> domain;
        Map<String, Object> mbean = null;
        if (listCache && stack.isEmpty()) {
            cache = getOrCreateJSONObject(infoMap, "cache");
            domains = getOrCreateJSONObject(infoMap, "domains");
            domain = getOrCreateJSONObject(domains, domainName);
        } else {
            domain = getOrCreateJSONObject(infoMap, domainName);
            mbean = getOrCreateJSONObject(domain, mbeanKeyListing);
        }

        if (stack.isEmpty()) {
            if (!listCache) {
                // normal JSONification of MBeanInfo
                addFullMBeanInfo(mbean, objectName, mBeanInfo, objectName, customUpdaters);
            } else {
                // cached MBeanInfo
                String key = null;
                for (CacheKeyProvider provider : cacheKeyProviders) {
                    key = provider.determineKey(pInstance);
                    if (key != null) {
                        break;
                    }
                }
                if (key != null && cache != null) {
                    // an MBean may share its JSONified MBeanInfo with other MBeans
                    // object name points to a key
                    domain.put(mbeanKeyListing, key);
                    // while key points to shared JSONified MBeanInfo
                    mbean = getOrCreateJSONObject(cache, key);
                    if (mbean.isEmpty()) {
                        addFullMBeanInfo(mbean, objectName, mBeanInfo, objectName, customUpdaters);
                    }
                } else {
                    // back to normal behavior
                    mbean = getOrCreateJSONObject(domain, mbeanKeyListing);
                    addFullMBeanInfo(mbean, objectName, mBeanInfo, objectName, customUpdaters);
                }
            }
        } else {
            addPartialMBeanInfo(mbean, objectName, mBeanInfo, objectName, stack);
        }
        // Trim if required
        if (mbean != null && mbean.isEmpty()) {
            domain.remove(mbeanKeyListing);
            if (domain.isEmpty()) {
                infoMap.remove(domainName);
            }
        }
    }

    private String addProviderIfNeeded(String pDomain) {
        return pProvider != null ? pProvider + "@" + pDomain : pDomain;
    }

    /**
     * Add an exception which occurred during extraction of an {@link MBeanInfo} for
     * a certain {@link ObjectName} to this map.
     *
     * @param pName MBean name for which the error occurred
     * @param pExp exception occurred
     * @throws IOException if this method decides to rethrow the execption
     */
    public void handleException(ObjectName pName, IOException pExp) throws IOException {
        // In case of a remote call, IOException can occur e.g. for
        // NonSerializableExceptions
        if (pathStack.isEmpty()) {
            addException(pName, pExp);
        } else {
            // Happens for a deeper request, i.e. with a path pointing directly into an MBean,
            // Hence we throw immediately an error here since there will be only this exception
            // and no extra info
            throw new IOException("IOException for MBean " + pName + " (" + pExp.getMessage() + ")",pExp);
        }
    }

    /**
     * Add an exception which occurred during extraction of an {@link MBeanInfo} for
     * a certain {@link ObjectName} to this map.
     *
     * @param pName MBean name for which the error occurred
     * @param pExp exception occurred
     * @throws IllegalStateException if this method decides to rethrow the exception
     */
    public void handleException(ObjectName pName, IllegalStateException pExp) {
        // This happen happens for JBoss 7.1 in some cases.
        if (pathStack.isEmpty()) {
            addException(pName, pExp);
        } else {
            throw new IllegalStateException("IllegalStateException for MBean " + pName + " (" + pExp.getMessage() + ")",pExp);
        }
    }

    /**
     * Add an exception which occurred during extraction of an {@link MBeanInfo} for
     * a certain {@link ObjectName} to this map.
     *
     * @param pName MBean name for which the error occurred
     * @param pExp exception occurred
     * @throws IllegalStateException if this method decides to rethrow the exception
     */
    public void handleException(ObjectName pName, InstanceNotFoundException pExp) throws InstanceNotFoundException {
        // This happen happens for JBoss 7.1 in some cases (i.e. ResourceAdapterModule)
        if (pathStack.isEmpty()) {
           addException(pName, pExp);
        } else {
           throw new InstanceNotFoundException("InstanceNotFoundException for MBean " + pName + " (" + pExp.getMessage() + ")");
        }
    }

    // Add an exception to the info map
    private void addException(ObjectName pName, Exception pExp) {
        Map<String, Object> domain = getOrCreateJSONObject(infoMap, addProviderIfNeeded(pName.getDomain()));
        Map<String, Object> mbean = getOrCreateJSONObject(domain, getKeyPropertyString(pName));
        mbean.put(DataKeys.ERROR.getKey(), pExp.toString());
    }

    /**
     * Extract either a subtree or a leaf value. If a path is used, then adding MBeanInfos has added them
     * as if no path were given (i.e. in it original place in the tree) but leaves out other information
     * not included by the path. This method then moves up the part pointed to by the path to the top of the
     * tree hierarchy. It also takes into account the maximum depth of the tree and truncates below
     *
     * @return either a Map for a subtree or the leaf value as an object
     */
    public Object applyPath() {
        Object value = navigatePath();
        if (maxDepth == 0) {
            return value;
        }
        if (! (value instanceof JSONObject)) {
            return value;
        } else {
            // Truncate all levels below
            return truncateJSONObject((JSONObject) value, maxDepth);
        }
    }

    // =====================================================================================================

    /**
     * Populates JSON MBean information based on {@link MBeanInfo} using all available {@link DataUpdater updaters}.
     *
     * @param pObjectName
     * @param pMBeanMap
     * @param pMBeanInfo
     * @param pName
     * @param customUpdaters
     */
    private void addFullMBeanInfo(Map<String, Object> pMBeanMap, ObjectName pObjectName, MBeanInfo pMBeanInfo, ObjectName pName, Set<DataUpdater> customUpdaters) {
        for (DataUpdater updater : UPDATERS.values()) {
            updater.update(pMBeanMap, pObjectName, pMBeanInfo, null);
        }
        if (listKeys) {
            LIST_KEYS_UPDATER.update(pMBeanMap, pObjectName, pMBeanInfo, null);
        }
        for (DataUpdater customUpdater : customUpdaters) {
            customUpdater.update(pMBeanMap, pObjectName, pMBeanInfo, null);
        }
    }

    /**
     * Populates JSON MBean information based on {@link MBeanInfo} using selected {@link DataUpdater updater}.
     *
     * @param pObjectName
     * @param pMBeanMap
     * @param pMBeanInfo
     * @param pName
     */
    private void addPartialMBeanInfo(Map<String, Object> pMBeanMap, ObjectName pObjectName, MBeanInfo pMBeanInfo, ObjectName pName, Deque<String> pPathStack) {
        String what = pPathStack.isEmpty() ? null : pPathStack.pop();
        DataUpdater updater = UPDATERS.get(what);
        if (updater == null && "keys".equals(what)) {
            updater = LIST_KEYS_UPDATER;
        }
        if (updater != null) {
            updater.update(pMBeanMap, pObjectName, pMBeanInfo, pPathStack);
        } else {
            throw new IllegalArgumentException("Illegal path element " + what);
        }
    }

    /**
     * Ensure that {@code pMap} contains a nested map under {@code pKey} key and returns such nested
     * {@link JSONObject}.
     * @param pMap
     * @param pKey
     * @return
     */
    private Map<String, Object> getOrCreateJSONObject(Map<String, Object> pMap, String pKey) {
        JSONObject nMap = (JSONObject) pMap.get(pKey);
        if (nMap == null) {
            nMap = new JSONObject();
            pMap.put(pKey, nMap);
        }
        return nMap;
    }

    private Object truncateJSONObject(JSONObject pValue, int pMaxDepth) {
        if (pMaxDepth == 0) {
            return 1;
        }
        JSONObject ret = new JSONObject();
        Set<Map.Entry<String, Object>> entries = pValue.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof JSONObject) {
                ret.put(key, truncateJSONObject((JSONObject) value, pMaxDepth - 1));
            } else {
                ret.put(key,value);
            }
        }
        return ret;
    }

    // Trim down the stack by some value or return an empty stack
    private Deque<String> truncatePathStack(int pLevel) {
        if (pathStack.size() < pLevel) {
            return new LinkedList<>();
        } else {
            // Trim of domain and MBean properties
            // pathStack gets cloned here since the processing will eat it up
            Deque<String> ret = new LinkedList<>(pathStack);
            for (int i = 0;i < pLevel;i++) {
                ret.pop();
            }
            return ret;
        }
    }

    // Navigate to sub map or leaf value
    private Object navigatePath() {
        int size = pathStack.size();
        Map<String, Object> innerMap = infoMap;

        while (size > 0) {
            Collection<Object> vals = innerMap.values();
            if (vals.isEmpty()) {
                return innerMap;
            } else if (vals.size() != 1) {
                throw new IllegalStateException("Internal: More than one key found when extracting with path: " + vals);
            }
            Object value = vals.iterator().next();

            // End leaf, return it ....
            if (size == 1) {
                return value;
            }
            // Dive in deeper ...
            if (!(value instanceof JSONObject)) {
                throw new IllegalStateException("Internal: Value within path extraction must be a Map, not " + value.getClass());
            }
            innerMap = (JSONObject) value;
            --size;
        }
        return innerMap;
    }
}
