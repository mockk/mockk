package io.mockk.quartz;

/**
 * Simplified AbstractTrigger mimicking Quartz's AbstractTrigger.
 * Implements both MutableTrigger (which has clone()) and the base functionality.
 */
public abstract class AbstractTrigger<T extends Trigger> implements MutableTrigger {
    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object clone() {
        try {
            AbstractTrigger<?> copy = (AbstractTrigger<?>) super.clone();
            return copy;
        } catch (CloneNotSupportedException ex) {
            throw new IncompatibleClassChangeError("Not Cloneable.");
        }
    }
}
