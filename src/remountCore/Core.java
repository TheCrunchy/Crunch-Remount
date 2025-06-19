package remountCore;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
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

	    if (isPlayerAllowedToMount(player, mount)) {
	        event.setCancelled(false);
	    	
	    }

		 if (!event.isCancelled()) {
		     savePlayerToMountList(player, mount);
		 }
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onVehicleEnter(VehicleEnterEvent event) {

	    if (!(event.getEntered() instanceof Player)) return;
	    Player player = (Player) event.getEntered();
	    Entity vehicle = event.getVehicle();
        
	    if (isPlayerAllowedToMount(player, vehicle)) {
	        event.setCancelled(false);
	
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
