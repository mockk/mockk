package io.mockk.quartz;

/**
 * Simplified SimpleTriggerImpl mimicking Quartz's SimpleTriggerImpl.
 * The complex inheritance hierarchy triggers the "Cannot infer visibility" error
 * when MockK tries to determine if methods are Kotlin inline functions.
 */
public class SimpleTriggerImpl extends AbstractTrigger<SimpleTrigger> implements SimpleTrigger {
    private int timesTriggered = 0;

    @Override
    public int getTimesTriggered() {
        return timesTriggered;
    }

    public void setTimesTriggered(int timesTriggered) {
        this.timesTriggered = timesTriggered;
    }
}
