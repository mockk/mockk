package io.mockk.it;

/**
 * Minimal replica of the Quartz scheduler Trigger hierarchy (issue #1432): the base
 * interface extends Cloneable while clone() is only declared on a subinterface, and the
 * implementation also inherits Cloneable through a second interface that does not declare
 * clone(). kotlin-reflect cannot analyze such classes and fails with
 * "Cannot infer visibility for inherited open fun clone".
 */
public class CloneDeclaredOnSubInterface {
    public interface Trigger extends Cloneable {
        void trigger();
    }

    public interface MutableTrigger extends Trigger {
        Object clone();
    }

    public interface SimpleTrigger extends Trigger {
        int getTimesTriggered();
    }

    public abstract static class AbstractTrigger implements MutableTrigger {
        @Override
        public Object clone() {
            return null;
        }
    }

    public static class SimpleTriggerImpl extends AbstractTrigger implements SimpleTrigger {
        private int timesTriggered = 0;

        @Override
        public int getTimesTriggered() {
            return timesTriggered;
        }

        @Override
        public void trigger() {
            timesTriggered++;
        }
    }
}
