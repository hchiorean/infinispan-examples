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

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.CacheImpl;

/**
 * Node1.
 *
 * @author Martin Gencur
 */
public class Node1 extends AbstractNode {

    static final List<String> BIKE_IDS = Arrays.asList("bike1", "bike2", "bike3");

    private static final Random RND = new Random();

    private static final String initialPrompt = "Choose action:\n" + "============= \n"
            + "u   -  update some of bike components\n" + "p   -  print current bike components\n" + "q   -  quit\n";

    public static void main( String[] args ) throws Exception {
        new Node1().run();
    }

    public void run() throws Exception {
        Cache<String, Bicycle> cache = bicyclesCache();
        TransactionManager tm = ((CacheImpl)cache).getAdvancedCache().getTransactionManager();

        waitForClusterToForm();

        // put some information in the cache that we can display on the other node
        loadInitialData(cache, tm);

        Console con = System.console();
        con.printf(initialPrompt);

        while (true) {
            String action = con.readLine(">");

            if ("p".equals(action)) {
                printCacheContents(cache);
            } else if ("u".equals(action)) {
                updateBicycleWhichIsNotInCache(cache, tm);
            } else if ("q".equals(action)) {
                System.exit(0);
            }
        }
    }

    private void updateBicycleWhichIsNotInCache( Cache<String, Bicycle> cache,
                                                 TransactionManager tm ) throws Exception {
        try {
            tm.begin();

            //retrieve a bicycle id which is not loaded in the cache atm
            List<String> bikeIdsNotInCache = new ArrayList<String>(BIKE_IDS);
            bikeIdsNotInCache.removeAll(loadedKeys(bicyclesCache()));
            if (bikeIdsNotInCache.isEmpty()) {
                System.out.println("WARN: Make sure eviction is turned on and has a value lower than the list of bikes");
                bikeIdsNotInCache = BIKE_IDS;
            }
            String bikeId = bikeIdsNotInCache.get(RND.nextInt(bikeIdsNotInCache.size()));

            Bicycle toChange = cache.get(bikeId);

            //apply some changes, only these changes will be replicated
            System.out.println("Updating components: frame, fork for bike: " + bikeId);
            String uuid = UUID.randomUUID().toString();
            toChange.setFrame("New Frame_" + uuid);
            toChange.setFork("New Fork_" + uuid);

            //store the bicycle back to the cache
            cache.put(bikeId, toChange);

            tm.commit();
        } catch (Exception e) {
            if (tm != null) {
                tm.rollback();
            }
            throw e;
        }
    }


    private void loadInitialData( Cache<String, Bicycle> cache,
                                  TransactionManager tm ) throws Exception {
        try {
            tm.begin();
            for (String bikeId : BIKE_IDS) {
                Bicycle bike = new Bicycle();
                bike.initializeWithDefaults();
                cache.put(bikeId, bike);
            }
            tm.commit();
        } catch (Exception e) {
            if (tm != null) {
                tm.rollback();
            }
            throw e;
        }
    }

    @Override
    protected int getNodeId() {
        return 1;
    }
}
