package io.karp.scout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class VolleyListener implements Listener {

    public static final double VOLLEY_MULT = 3.3;
    public static final double VOLLEY_CD = 20;
    private Set<Projectile> volleyProjectiles;
    private Map<Player, Long> last_cast;

    public VolleyListener() {
        volleyProjectiles = new HashSet<Projectile>();
        last_cast = new HashMap<>();
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player)) return;
        
        Player shootingPlayer = (Player) shooter;
        if (!shootingPlayer.isSneaking()) return;

        long currTime = shootingPlayer.getWorld().getFullTime();
        if (last_cast.containsKey(shootingPlayer) && currTime < last_cast.get(shootingPlayer) + VOLLEY_CD) return;
        last_cast.put(shootingPlayer, currTime);

        Projectile initialShot = event.getEntity();
        Class<? extends Projectile> projectileClass = initialShot.getClass();

        for (int angleOffset = -90; angleOffset <= 90; angleOffset += 5) {
            Location shootingLocation = shootingPlayer.getLocation().clone();
            shootingLocation.setPitch(shootingLocation.getPitch() + 90);
            Vector volleyProjectileVelocity = initialShot.getVelocity().clone().rotateAroundAxis(shootingLocation.getDirection(), angleOffset);
            
            Projectile volleyProjectile = shootingPlayer.launchProjectile(projectileClass, volleyProjectileVelocity);
            volleyProjectiles.add(volleyProjectile);
        }

        initialShot.remove();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!volleyProjectiles.contains(event.getDamageSource().getDirectEntity())) return;
        event.setDamage(event.getDamage() * VOLLEY_MULT);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!volleyProjectiles.contains(event.getEntity())) return;
        if (event.getHitBlock() == null) return;
        volleyProjectiles.remove(event.getEntity());
        event.getEntity().remove();
    }
}
