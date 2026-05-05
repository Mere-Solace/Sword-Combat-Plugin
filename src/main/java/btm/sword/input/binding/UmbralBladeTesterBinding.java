package btm.sword.input.binding;

import btm.sword.entity.player.DevSwordPlayer;
import btm.sword.input.InputType;
import btm.sword.item.core.KeyRegistry;

/**
 * EARLY-phase binding that wires the dev-only UmbralBlade lifecycle tester item
 * (a {@code BREEZE_ROD} tagged with {@link KeyRegistry#UMBRAL_BLADE_TESTER_KEY}) to
 * blade activate/deactivate commands.
 *
 * <ul>
 *   <li>{@link InputType#LEFT}: deactivate the blade.</li>
 *   <li>{@link InputType#DROP}: activate the blade.</li>
 * </ul>
 *
 * <p>Only fires for {@link DevSwordPlayer} — non-dev players holding the tester item
 * will fall through to the trie. Replaces the inline {@code handleUmbralBladeTesterInput}
 * branch of {@code DevSwordPlayer.act}.</p>
 */
public final class UmbralBladeTesterBinding implements ItemInputBinding {

    @Override
    public String id() {
        return "umbral_blade_tester";
    }

    @Override
    public Phase phase() {
        return Phase.EARLY;
    }

    @Override
    public boolean matches(MatchContext ctx) {
        if (!(ctx.player() instanceof DevSwordPlayer)) return false;
        if (ctx.input() != InputType.LEFT && ctx.input() != InputType.DROP) return false;
        return KeyRegistry.hasKey(ctx.item(), KeyRegistry.UMBRAL_BLADE_TESTER_KEY);
    }

    @Override
    public boolean dispatch(MatchContext ctx) {
        DevSwordPlayer dev = (DevSwordPlayer) ctx.player();
        switch (ctx.input()) {
            case LEFT -> dev.deactivateUmbralBlade();
            case DROP -> dev.activateUmbralBlade();
            default -> {
                return false;
            }
        }
        return true;
    }
}
