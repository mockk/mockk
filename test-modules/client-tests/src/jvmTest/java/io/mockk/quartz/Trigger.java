package io.mockk.quartz;

/**
 * Simplified Trigger interface mimicking Quartz's Trigger.
 * Key issue: Extends Cloneable but does NOT declare clone() method here.
 */
public interface Trigger extends Cloneable, java.io.Serializable {
    String getName();
}
