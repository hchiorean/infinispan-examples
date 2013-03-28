/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.examples.partialreplication;

import java.io.File;
import java.util.Map;
import java.util.Set;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.examples.partialreplication.util.ClusterValidation;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;

/**
 * AbstractNode.
 *
 * @author Pete Muir
 * @author Martin Gencur
 */
@SuppressWarnings("unused")
public abstract class AbstractNode {

    private EmbeddedCacheManager createCacheManagerProgramatically() {
        return new DefaultCacheManager(
                GlobalConfigurationBuilder.defaultClusteredBuilder()
                                          .transport().addProperty("configurationFile", "jgroups.xml")
                                          .serialization()
                                          .addAdvancedExternalizer(new BicycleDelta.Externalizer())
                                          .addAdvancedExternalizer(new Bicycle.Externalizer())
                                          .build(),
                new ConfigurationBuilder()
                        .loaders()
                            .addFileCacheStore()
                            .location(filestoreLocation())
                            .fetchPersistentState(false)
                        .clustering().cacheMode(CacheMode.REPL_SYNC)
                        .transaction()
                            .transactionMode(TransactionMode.TRANSACTIONAL)
                            .lockingMode(LockingMode.PESSIMISTIC)
                        .transactionManagerLookup(new JBossStandaloneJTAManagerLookup())
                        .eviction()
                            .maxEntries(1)
                            .strategy(EvictionStrategy.LRU)
                        .build()
        );
    }

    public static final int CLUSTER_SIZE = 2;

    private final EmbeddedCacheManager cacheManager;

    public AbstractNode() {
        delete(new File(filestoreLocation()));

        this.cacheManager = createCacheManagerProgramatically();
    }

    protected EmbeddedCacheManager getCacheManager() {
        return cacheManager;
    }

    protected void waitForClusterToForm() {
        // Wait for the cluster to form, erroring if it doesn't form after the timeout
        if (!ClusterValidation.waitForClusterToForm(getCacheManager(), getNodeId(), CLUSTER_SIZE)) {
            throw new IllegalStateException("Error forming cluster, check the log");
        }
    }

    protected abstract int getNodeId();

    protected void printCacheContents( Cache<String, Bicycle> cache ) {
        for (Map.Entry<String, Bicycle> entry : cache.entrySet()) {
            System.out.println("======== BIKE " + entry.getKey() + " ========\n");
            System.out.println(entry.getValue());
            System.out.println("=====================\n");
        }
    }

    protected Set<String> loadedKeys(Cache<String, Bicycle> cache) {
        return cache.keySet();
    }

    protected Cache<String, Bicycle> bicyclesCache() {
        return getCacheManager().getCache("Bicycles");
    }

    protected String filestoreLocation() {
        return "target/"+ getNodeId();
    }


    /**
     * Delete the file or directory given by the supplied reference. This method works on a directory that is not empty, unlike
     * the {@link File#delete()} method.
     *
     * @param fileOrDirectory the reference to the Java File object that is to be deleted
     * @return true if the supplied file or directory existed and was successfully deleted, or false otherwise
     */
    public static boolean delete( File fileOrDirectory ) {
        if (fileOrDirectory == null) return false;
        if (!fileOrDirectory.exists()) return false;

        // The file/directory exists, so if a directory delete all of the contents ...
        if (fileOrDirectory.isDirectory()) {
            for (File childFile : fileOrDirectory.listFiles()) {
                delete(childFile); // recursive call (good enough for now until we need something better)
            }
            // Now an empty directory ...
        }
        // Whether this is a file or empty directory, just delete it ...
        return fileOrDirectory.delete();
    }
}
