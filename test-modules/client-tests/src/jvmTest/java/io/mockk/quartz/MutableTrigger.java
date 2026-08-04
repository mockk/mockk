package io.mockk.quartz;

/**
 * Simplified MutableTrigger interface mimicking Quartz's MutableTrigger.
 * Key issue: Declares clone() method even though Cloneable is implemented in parent.
 */
public interface MutableTrigger extends Trigger {
    Object clone();
    void setName(String name);
}
