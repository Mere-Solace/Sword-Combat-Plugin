package btm.sword.system.inventory.selection;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CustomActionButton extends MenuButton {
	private final Consumer<InventoryClickEvent> onClick;
	private final UUID buttonUUID;
	
	public CustomActionButton(Material itemType, Component name, List<Component> lore, Consumer<InventoryClickEvent> onClick) {
		super(itemType, name, lore);
		this.onClick = onClick;
		buttonUUID = UUID.randomUUID();
	}
	
	@Override
	public void onClick(InventoryClickEvent e) {
		onClick.accept(e);
	}
	
	@Override
	public @NotNull String getId() {
		return buttonUUID.toString();
	}
}
