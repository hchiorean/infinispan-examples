Partial state transfer example
==============================

To compile, type:

    `mvn clean compile dependency:copy-dependencies -DstripVersion`
     
and then, to run (in two different terminals):

    `java -cp target/classes:target/dependency/* org.infinispan.examples.partialreplication.Node0` 

and

    `java -cp target/classes:target/dependency/* org.infinispan.examples.partialreplication.Node1`


Node0 application will listen for changes to a bicycle,
Node1 will make these changes.

Reproducing the issue
---------------------
1. start node0.
2. start node1.

After both nodes have started, you should see some "Updated components: fork, rearShock, frame, crank" messages in node0's
terminal, indicating that the initial state transfer was successful and all "bike state" has been passed to node0's cache.

3. type "p" in node0 to see current bicycle from the cache.
4. type "p" in node1 to see current bicycle from the cache.

Both views should be identical

5. type "u" in node1 to update a random bicycle from the bicycles list, other than the one which was loaded at the beginning.

Only the frame and fork components will be changed. Look at the console of node0 -> you should see "Updated components: fork, frame"

6. type "p" in node1 to see the updated bicycle.
7. type "p" in node0.

You should see the "frame" & "fork" with updated values, but "rearShock" and "crank" empty, indicating that
the values for those fields have been "lost".