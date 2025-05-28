package remountCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityMountEvent;
public class Core extends JavaPlugin {
	public static Plugin plugin;
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new MountEvent(), this);
		plugin = this;
	}

	public class MountEvent implements Listener {

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
		public void onEntityMount(EntityMountEvent event) {
		    if (!(event.getEntity() instanceof Player)) return;

		    Player player = (Player) event.getEntity();
		    Entity mount = event.getMount();

		    if (!event.isCancelled()) {
		        savePlayerToMountList(player, mount);
		        return;
		    }

		    NamespacedKey key = new NamespacedKey(Core.plugin, "last_mounts");
		    PersistentDataContainer container = mount.getPersistentDataContainer();
		    if (!container.has(key, PersistentDataType.STRING)) return;

		    String uuid = player.getUniqueId().toString();
		    String raw = container.get(key, PersistentDataType.STRING);

		    long now = System.currentTimeMillis();
		    final long THREE_HOURS = 3 * 60 * 60 * 1000;

		    
		    for (String entry : raw.split(",")) {
		        String[] parts = entry.split(":");
		        if (parts.length != 2) continue;

		        if (parts[0].equals(uuid)) {
		            long time = Long.parseLong(parts[1]);
		            if (now - time <= THREE_HOURS) {
		                event.setCancelled(false);
		                player.sendMessage("You're allowed to mount again (within 3 hours).");
		            } else {
		                player.sendMessage("Your previous mount was over 3 hours ago.");
		            }
		            break;
		        }
		    }
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
