package btm.sword.system.statemachine;

import java.util.Map;
import java.util.function.Predicate;

public interface State<T> {
    String name();
    void onEnter(T context);
    void onExit(T context);
    void onTick(T context);
    Map<Class<? extends State<T>>, Predicate<T>> transitions();
}
