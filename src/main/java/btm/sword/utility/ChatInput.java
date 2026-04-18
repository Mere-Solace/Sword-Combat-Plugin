package btm.sword.utility;

import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Typed wrapper over {@link ChatInputCapture} for prompting the player to type
 * a value in chat and receiving a parsed result.
 *
 * <p>Each {@code prompt*} helper:</p>
 * <ul>
 *   <li>Supersedes any currently pending capture for the same player (the
 *       previous callback is cancelled).</li>
 *   <li>Sends a labelled prompt with the current value and format hint.</li>
 *   <li>On {@code cancel} or empty input, invokes {@code returnTo} without
 *       calling the value callback.</li>
 *   <li>On parse failure, re-prompts the same player without spam.</li>
 *   <li>On success, invokes the callback then {@code returnTo} on the main
 *       thread.</li>
 * </ul>
 */
public final class ChatInput {

    private ChatInput() {}

    /** Prompts for an integer value. */
    public static void promptInt(Player player, String label, int current,
        Consumer<Integer> onValue, Runnable returnTo) {
        promptTyped(player, label, String.valueOf(current), "integer", Integer::parseInt,
            onValue, returnTo);
    }

    /** Prompts for a float value. */
    public static void promptFloat(Player player, String label, float current,
        Consumer<Float> onValue, Runnable returnTo) {
        promptTyped(player, label, String.format("%.3f", current), "decimal",
            Float::parseFloat, onValue, returnTo);
    }

    /** Prompts for a double value. */
    public static void promptDouble(Player player, String label, double current,
        Consumer<Double> onValue, Runnable returnTo) {
        promptTyped(player, label, String.format("%.3f", current), "decimal",
            Double::parseDouble, onValue, returnTo);
    }

    /** Prompts for a trimmed string value; empty input counts as cancel. */
    public static void promptString(Player player, String label, String current,
        Consumer<String> onValue, Runnable returnTo) {
        promptTyped(player, label, current, "text", s -> s, onValue, returnTo);
    }

    private static <T> void promptTyped(Player player, String label, String currentDisplay,
        String format, Function<String, T> parser,
        Consumer<T> onValue, Runnable returnTo) {

        if (ChatInputCapture.hasPending(player)) {
            ChatInputCapture.cancel(player);
        }

        Component prompt = Component.text(label + ": ", NamedTextColor.AQUA)
            .append(Component.text("(current: " + currentDisplay + ", type a "
                + format + ")", NamedTextColor.GRAY));

        register(player, prompt, parser, onValue, returnTo);
    }

    private static <T> void register(Player player, Component prompt,
        Function<String, T> parser, Consumer<T> onValue, Runnable returnTo) {

        ChatInputCapture.prompt(player, prompt, text -> {
            String trimmed = text.trim();
            if (trimmed.isEmpty() || "cancel".equalsIgnoreCase(trimmed)) {
                returnTo.run();
                return;
            }
            try {
                T value = parser.apply(trimmed);
                onValue.accept(value);
                returnTo.run();
            } catch (RuntimeException ex) {
                player.sendMessage(Component.text("Invalid input — try again.",
                    NamedTextColor.RED));
                register(player, prompt, parser, onValue, returnTo);
            }
        });
    }
}
