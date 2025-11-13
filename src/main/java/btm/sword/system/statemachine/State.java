package btm.sword.system.statemachine;

public abstract class State<T> {
    public abstract String name();
    public abstract void onEnter(T context);
    public abstract void onExit(T context);
    public abstract void onTick(T context);
}
