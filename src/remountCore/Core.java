package remountCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityMountEvent;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import net.minecraft.nbt.NBTTagCompound;

public class Core extends JavaPlugin {
	public static Plugin plugin;
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new MountEvent(), this);
		plugin = this;
	}
	
	
	
	public class MountEvent implements Listener {
		
//		private final Map<UUID, ItemStack> playerItemCache = new HashMap<>();
//
//		@EventHandler
//		public void onInventoryClick(InventoryClickEvent event) {
//		    if (!(event.getWhoClicked() instanceof Player)) return;
//		    getLogger().info("Clicked");
//		    Player player = (Player) event.getWhoClicked();
//		    ItemStack clicked = event.getCurrentItem();
//
//		    if (clicked == null || clicked.getType() == Material.AIR) return;
//
//		    // Check if the item has NBT data using NBT API
//		    NBTItem nbtClicked = new NBTItem(clicked);
//		    if (nbtClicked.getKeys().isEmpty()) {
//		        // No NBT data, don't cache
//		        playerItemCache.remove(player.getUniqueId());
//		        return;
//		    }
//
//		    // Cache the full ItemStack (clone to avoid modifying original)
//		    playerItemCache.put(player.getUniqueId(), clicked.clone());
//		}
//
//		@EventHandler
//		public void onInventoryDrag(InventoryDragEvent event) {
//		    if (!(event.getWhoClicked() instanceof Player)) return;
//		    Player player = (Player) event.getWhoClicked();
//
//		    ItemStack cachedItem = playerItemCache.get(player.getUniqueId());
//		    if (cachedItem == null) return;
//
//		    InventoryView view = player.getOpenInventory();
//		    Inventory topInv = view.getTopInventory();
//		    Inventory bottomInv = view.getBottomInventory();
//
//		    NBTItem cachedNBT = new NBTItem(cachedItem);
//		    if (cachedNBT.getKeys().isEmpty()) return;
//		    for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
//		        int rawSlot = entry.getKey();
//		        ItemStack draggedItem = entry.getValue();
//		        if (draggedItem == null || draggedItem.getType() == Material.AIR) continue;
//
//		        if (draggedItem.getType() == cachedItem.getType()) {
//		            ItemStack replaced = cachedItem.clone();
//		            replaced.setAmount(draggedItem.getAmount());
//
//		            int convertedSlot = view.convertSlot(rawSlot);
//
//		            Inventory targetInventory;
//		            if (convertedSlot < topInv.getSize()) {
//		                targetInventory = topInv;
//		            } else {
//		                int playerSlot = convertedSlot;
//		                if (playerSlot < bottomInv.getSize()) {
//		                    targetInventory = bottomInv;
//		                    convertedSlot = playerSlot;
//		                } else {
//		                    continue; // Outside inventories, skip
//		                }
//		            }
//
//		            if (convertedSlot >= 0 && convertedSlot < targetInventory.getSize()) {
//		                final int slotToSet = convertedSlot;
//		                final Inventory invToSet = targetInventory;
//		                final ItemStack itemToSet = replaced;
//
//		                Core.plugin.getServer().getScheduler().runTaskLater(Core.plugin, () -> {
//		                    invToSet.setItem(slotToSet, itemToSet);
//		                    getLogger().info("Reapplied cached NBT item to slot " + slotToSet + " in " 
//		                        + (invToSet == topInv ? "top" : "bottom") + " inventory with amount " + itemToSet.getAmount());
//		                }, 1L);
//		            } else {
//		                getLogger().info("Invalid converted slot: " + convertedSlot);
//		            }
//		        }
//		    }
//		}
//
//		
//		@EventHandler
//		public void onInventoryClose(InventoryCloseEvent event) {
//			playerItemCache.remove(event.getPlayer().getUniqueId());
//		}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
	    Entity mount = event.getRightClicked();
	    Player player = event.getPlayer();

	    if (isPlayerAllowedToMount(player, mount)) {
	        event.setCancelled(false);
	    }
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onEntityMount(EntityMountEvent event) {
	    if (!(event.getEntity() instanceof Player)) return;

	    Player player = (Player) event.getEntity();
	    Entity mount = event.getMount();

	    if (isPlayerAllowedToMount(player, mount) && event.isCancelled()) {
	        event.setCancelled(false);
	    }

		 if (!event.isCancelled()) {
		     savePlayerToMountList(player, mount);
		 }
	}

	private boolean isPlayerAllowedToMount(Player player, Entity mount) {
	    NamespacedKey key = new NamespacedKey(Core.plugin, "last_mounts");
	    PersistentDataContainer container = mount.getPersistentDataContainer();
	    if (!container.has(key, PersistentDataType.STRING)) return false;

	    String uuid = player.getUniqueId().toString();
	    String raw = container.get(key, PersistentDataType.STRING);

	    long now = System.currentTimeMillis();
	    final long HALF_HOUR = 30 * 60 * 1000;

	    for (String entry : raw.split(",")) {
	        String[] parts = entry.split(":");
	        if (parts.length != 2) continue;

	        if (parts[0].equals(uuid)) {
	            long time = Long.parseLong(parts[1]);
	            return (now - time <= HALF_HOUR);
	        }
	    }
	    return false;
	}

		
	    //Store the last 5 players mounting it to the entitys state container 
		public void savePlayerToMountList(Player player, Entity mount) {
		    NamespacedKey key = new NamespacedKey(Core.plugin, "last_mounts");
		    PersistentDataContainer container = mount.getPersistentDataContainer();

		    long now = System.currentTimeMillis();
		    String uuid = player.getUniqueId().toString();

		    //parse existing data 
		    LinkedHashMap<String, Long> map = new LinkedHashMap<>();
		    if (container.has(key, PersistentDataType.STRING)) {
		        String raw = container.get(key, PersistentDataType.STRING);
		        for (String entry : raw.split(",")) {
		            String[] parts = entry.split(":");
		            if (parts.length == 2) {
		                map.put(parts[0], Long.parseLong(parts[1]));
		            }
		        }
		    }

		    // Update/add this player
		    map.remove(uuid); 
		    map.put(uuid, now); 

		    // Keep only the last 10
		    while (map.size() > 10) {
		        Iterator<String> it = map.keySet().iterator();
		        it.next();
		        it.remove();
		    }

		    StringBuilder sb = new StringBuilder();
		    for (Map.Entry<String, Long> entry : map.entrySet()) {
		        sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
		    }
		    if (sb.length() > 0) sb.setLength(sb.length() - 1); // trim trailing comma
		    container.set(key, PersistentDataType.STRING, sb.toString());
		}


	}
}
