package io.mockk.quartz;

/**
 * Simplified SimpleTrigger interface mimicking Quartz's SimpleTrigger.
 */
public interface SimpleTrigger extends Trigger {
    int getTimesTriggered();
}
