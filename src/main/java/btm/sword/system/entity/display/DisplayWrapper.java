package btm.sword.system.entity.display;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.bukkit.entity.Display;

public class DisplayWrapper {
    private Display display;

    public DisplayWrapper() {}

    public void shrink(float rate, float threshold, Function<Integer, Integer> period, TimeUnit unit, int max, ShrinkType type) {
//		for
//		SwordScheduler.runLater(new BukkitRunnable() {
//			@Override
//			public void run() {
//				final float check = 0.07f*xScale;
//				final float rate = 0.7f;
//				@Override
//				public void run() {
//					Transformation cur = bd.getTransformation();
//					Vector3f s = cur.getScale().mul(rate);
//					bd.setTransformation(
//							new Transformation(
//									new Vector3f(base).sub(new Vector3f(s).mul(0.5f)),
//									cur.getLeftRotation(),
//									s,
//									cur.getRightRotation()
//							)
//					);
//					if (s.x() < check) {
//						bd.remove();
//						cancel();
//					}
//				}
//			}
//		}, period.apply(t), unit);
    }
}
