package btm.sword.system.action.skill.type.impl.active;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.system.action.skill.SkillId;
import btm.sword.system.action.skill.SkillIds;
import btm.sword.system.action.skill.SkillType;
import btm.sword.system.action.skill.type.impl.charge.ChargeSession;
import btm.sword.system.action.skill.type.impl.charge.ChargeableAbility;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * A chargeable ice spell that launches a block of ice.
 *
 * <p>While charging, the display scales from {@code 0.25} to {@code 3.0},
 * rotates continuously, and bobs up and down with a sine wave.
 * The player may release once the scale reaches {@code 0.5}.</p>
 */
public class IceSpellAbility extends ChargeableAbility {

    private static final Component NAME = Component.text("Ice Bolt", NamedTextColor.AQUA);

    // ── Charge parameters ──────────────────────────────────────────────

    private static final float MIN_SCALE = 0.25f;
    private static final float MAX_SCALE = 3.0f;
    private static final float MIN_RELEASE_SCALE = 0.5f;
    private static final int CHARGE_TIME_MS = 3000;
    private static final double VELOCITY = 1.8;

    // ── Visual animation constants ─────────────────────────────────────

    /** Rotation speed in radians per millisecond. */
    private static final float ROTATION_SPEED = 0.003f;

    /** Sine wave amplitude (blocks) for the vertical bob. */
    private static final float BOB_AMPLITUDE = 0.15f;

    /** Sine wave period in milliseconds. */
    private static final float BOB_PERIOD_MS = 800f;

    // ── Session data keys ──────────────────────────────────────────────

    private static final String KEY_SCALE = "scale";

    // ── Skill identity ─────────────────────────────────────────────────

    @Override
    public Component name() {
        return NAME;
    }

    @Override
    public SkillId id() {
        return SkillIds.ICE_SPELL;
    }

    @Override
    public SkillType type() {
        return SkillType.ACTIVE;
    }

    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.BLUE_ICE)
            .name(NAME)
            .hideAll()
            .build();
    }

    @Override
    public List<Component> description() {
        return List.of(
            Component.text("Hold right-click to charge a block of ice.", NamedTextColor.GRAY),
            Component.text("Release to hurl it at your target.", NamedTextColor.DARK_GRAY)
        );
    }

    @Override
    public ItemStack buildWorldItem() {
        return ItemStackBuilder.of(Material.BLUE_ICE)
            .name(NAME)
            .lore(description())
            .hideAll()
            .build();
    }

    // ── Ability parameters ─────────────────────────────────────────────

    @Override
    public int maxUses() {
        return 5;
    }

    @Override
    public int cooldownTicks() {
        return 20;
    }

    @Override
    public double projectileVelocity() {
        return VELOCITY;
    }

    // ── Charge behavior ────────────────────────────────────────────────

    @Override
    public void onChargeTick(ChargeSession session, long elapsedMs) {
        ItemDisplay display = session.getDisplay();

        // Scale grows linearly from MIN to MAX over CHARGE_TIME_MS
        float progress = Math.min(1.0f, (float) elapsedMs / CHARGE_TIME_MS);
        float scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * progress;
        session.setFloat(KEY_SCALE, scale);

        // Continuous Y-axis rotation
        float angle = (elapsedMs * ROTATION_SPEED) % ((float) Math.PI * 2);
        Quaternionf rotation = new Quaternionf(new AxisAngle4f(angle, 0f, 1f, 0f));

        // Sine-wave vertical bob
        float bob = BOB_AMPLITUDE * (float) Math.sin(2.0 * Math.PI * elapsedMs / BOB_PERIOD_MS);

        display.setTransformation(new Transformation(
            new Vector3f(0f, bob, 0f),
            rotation,
            new Vector3f(scale, scale, scale),
            new Quaternionf()
        ));
    }

    @Override
    public boolean canRelease(ChargeSession session) {
        return session.getFloat(KEY_SCALE, 0f) >= MIN_RELEASE_SCALE;
    }

    @Override
    public float releaseDisplayScale(ChargeSession session) {
        return session.getFloat(KEY_SCALE, 1.0f);
    }

    // ── ActiveSkill contract ───────────────────────────────────────────

    @Override
    public void execute(Combatant combatant) {
        // Charge release is handled by ChargeAction.releaseCharge — execute() is not
        // called directly for chargeables. No-op here for safety.
    }

    @Override
    public int calculateCooldown(Combatant combatant) {
        return 500;
    }

    @Override
    public boolean canPerform(Combatant combatant) {
        return combatant.canPerformAction();
    }
}
