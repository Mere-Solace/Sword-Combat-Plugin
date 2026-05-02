package btm.sword.util.misc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import btm.sword.Sword;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Utility for capturing a single chat message from a player as typed input.
 * <p>
 * Used by interactive menus (e.g. the config editor) to prompt the player to
 * type a value in chat. The next message they send is intercepted, suppressed
 * from public chat, and routed to the registered callback on the main thread.
 * </p>
 * <p>
 * Call {@link #prompt} to register a capture. Call {@link #handle} from the
 * {@code AsyncChatEvent} handler to dispatch it. The callback always runs
 * synchronously via a scheduled task, so Bukkit API is safe to call within it.
 * </p>
 */
public final class ChatInputCapture {

    private static final Map<UUID, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    private ChatInputCapture() {}

    /**
     * Prompts the player to type a value in chat and registers a one-shot callback.
     * Closes the player's open inventory so they can type freely.
     *
     * @param player   the player to prompt
     * @param prompt   the prompt message sent to the player in chat
     * @param callback invoked on the main thread with the player's typed text
     */
    public static void prompt(Player player, Component prompt, Consumer<String> callback) {
        PENDING.put(player.getUniqueId(), callback);
        player.closeInventory();
        player.sendMessage(prompt);
        player.sendMessage(Component.text("(type 'cancel' to abort)", NamedTextColor.DARK_GRAY));
    }

    /**
     * Cancels any pending capture for the given player and notifies them.
     *
     * @param player the player whose capture to cancel
     */
    public static void cancel(Player player) {
        if (PENDING.remove(player.getUniqueId()) != null) {
            player.sendMessage(Component.text("Input cancelled.", NamedTextColor.GRAY));
        }
    }

    /**
     * Returns whether the given player has a pending capture.
     *
     * @param player the player to check
     * @return true if a capture is pending for this player
     */
    public static boolean hasPending(Player player) {
        return PENDING.containsKey(player.getUniqueId());
    }

    /**
     * Intercepts the chat event if the player has a pending capture.
     * <p>
     * Cancels the event (suppresses public chat output), extracts the plain-text
     * message, removes the pending capture, and schedules the callback on the
     * main server thread.
     * </p>
     *
     * @param event the async chat event from {@code PlayerListener}
     * @return true if the event was intercepted; false if no capture was pending
     */
    public static boolean handle(AsyncChatEvent event) {
        Consumer<String> callback = PENDING.remove(event.getPlayer().getUniqueId());
        if (callback == null) return false;
        event.setCancelled(true);
        Component msg = event.message();
        String text = (msg instanceof TextComponent tc ? tc.content() : msg.toString()).trim();
        Bukkit.getScheduler().runTask(Sword.getInstance(), () -> callback.accept(text));
        return true;
    }
}
