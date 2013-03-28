package org.infinispan.examples.partialreplication;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.marshall.AdvancedExternalizer;
import org.infinispan.util.Util;

/**
 * 
 * Bicycle consists of many components. In order to avoid replicating information about
 * all components every time the bicycle object is changed, we implement DeltaAware interface
 * which helps us replicate only changes that were made to some specific components.
 * 
 * @author Martin Gencur
 */
public class Bicycle implements DeltaAware, Cloneable {

    //bicycle attributes
    String frame = "", fork = "", rearShock = "", crank = "";

    private BicycleDelta delta;

    /**
     * When a transaction is committed, the delta is nulled and changes to that object are discarded.
     */
    @Override
    public void commit() {
        delta = null;
    }

    /**
     * Return data that should be replicated. Infinispan won't replicate the whole object but rather only the delta. 
     */
    @Override
    public Delta delta() {
        Delta toReturn = getDelta();
        delta = null; // reset
        return toReturn;
    }

    BicycleDelta getDelta() {
        if (delta == null) delta = new BicycleDelta();
        return delta;
    }

    public void initializeWithDefaults() {
        setFrame("All-New Mongoose Freedrive DH Aluminum 210mm travel");
        setFork("RockShox Boxxer RC w/200mm Travel, Maxle Lite DH 20mm thru-axle, Rebound & Low Speed Compression Adjust");
        setRearShock("Fox Van R w/210mm Travel, Rebound Adjust");
        setCrank("Truvativ Hussefelt 1.0 w/ E13 LG1 chainguide, 36t");
    }

    /* ========= getters/setters =============================== */

    public String getFrame() {
        return frame;
    }

    public void setFrame(String frame) {
        getDelta().registerComponentChange("frame", frame);
        this.frame = frame;
    }

    public String getFork() {
        return fork;
    }

    public void setFork(String fork) {
        getDelta().registerComponentChange("fork", fork);
        this.fork = fork;
    }

    public String getRearShock() {
        return rearShock;
    }

    public void setRearShock(String rearShock) {
        getDelta().registerComponentChange("rearShock", rearShock);
        this.rearShock = rearShock;
    }

    public String getCrank() {
        return crank;
    }

    public void setCrank(String crank) {
        getDelta().registerComponentChange("crank", crank);
        this.crank = crank;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("*** Bike components ***\n");
        sb.append("=======================");
        sb.append("\nframe: " + frame);
        sb.append("\nfork: " + fork);
        sb.append("\nrearShock: " + rearShock);
        sb.append("\ncrank: " + crank);
        return sb.toString();
    }

    /**
     * An externalizer that is used to marshall Bicycle objects
     */
    public static class Externalizer implements AdvancedExternalizer<Bicycle> {
        @Override
        public Set<Class<? extends Bicycle>> getTypeClasses() {
            return Util.<Class<? extends Bicycle>> asSet(Bicycle.class);
        }

        @Override
        public void writeObject(ObjectOutput output, Bicycle object) throws IOException {
            output.writeUTF(object.frame);
            output.writeUTF(object.fork);
            output.writeUTF(object.rearShock);
            output.writeUTF(object.crank);
        }
    
        @Override
        public Bicycle readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            Bicycle bicycle = new Bicycle();
            bicycle.frame = input.readUTF();
            bicycle.fork = input.readUTF();
            bicycle.rearShock = input.readUTF();
            bicycle.crank = input.readUTF();
            return bicycle;
        }

        @Override
        public Integer getId() {
            return 22; //put some random value here to identify the externalizer (must not be in reserved value ranges)
        }
    }
    
}
