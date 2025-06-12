package btm.sword.system.input;

import btm.sword.system.entity.SwordPlayer;

import java.util.function.Predicate;

public class FuncPredicate implements Predicate<SwordPlayer> {

	@Override
	public boolean test(SwordPlayer swordPlayer) {
		return false;
	}
}
