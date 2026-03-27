package btm.sword.system.action.skill.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillId;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Session-scoped log of ability events (uses and surrenders) for a single player.
 *
 * <p>Entries are appended chronologically and never reordered.
 * The full list is loaded from the database on login and saved on quit.
 */
public class PlayerAbilityHistory {

    private final List<AbilityHistoryEntry> entries;

    /** Constructs an empty history for a first-time player. */
    public PlayerAbilityHistory() {
        this.entries = new ArrayList<>();
    }

    /**
     * Constructs a history pre-populated from persisted data.
     *
     * @param entries the restored entry list (mutable copy is made internally)
     */
    public PlayerAbilityHistory(List<AbilityHistoryEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    /**
     * Records that a consumable or throwable ability was used (item consumed).
     *
     * @param skill the ability that was used
     */
    public void recordUse(Skill skill) {
        entries.add(new AbilityHistoryEntry(
            skill.id(),
            PlainTextComponentSerializer.plainText().serialize(skill.name()),
            AbilityHistoryAction.USED,
            System.currentTimeMillis()
        ));
    }

    /**
     * Records that a quest ability was surrendered.
     *
     * @param skill the ability that was surrendered
     */
    public void recordSurrender(Skill skill) {
        entries.add(new AbilityHistoryEntry(
            skill.id(),
            PlainTextComponentSerializer.plainText().serialize(skill.name()),
            AbilityHistoryAction.SURRENDERED,
            System.currentTimeMillis()
        ));
    }

    /**
     * Records an entry directly — used by the persistence layer on load.
     *
     * @param entry the pre-built entry to add
     */
    public void addEntry(AbilityHistoryEntry entry) {
        entries.add(entry);
    }

    /**
     * Returns an unmodifiable view of all history entries, oldest-first.
     *
     * @return the full history
     */
    public List<AbilityHistoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Filters entries to only those matching the given ability id.
     *
     * @param id the ability to filter by
     * @return a new list of matching entries
     */
    public List<AbilityHistoryEntry> entriesFor(SkillId id) {
        return entries.stream()
            .filter(e -> e.id().equals(id))
            .toList();
    }
}
