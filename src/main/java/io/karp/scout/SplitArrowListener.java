package io.karp.scout;

import java.util.Comparator;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class SplitArrowListener implements Listener {

    public static final double RADIUS = 10.0;
    public static final double SPLIT_ARROW_MULTIPLIER = 0.6;

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Precondition: Player hitting something alive with a projectile
        if (!(
            event.getDamageSource().getDamageType() == DamageType.ARROW ||
            event.getDamageSource().getDamageType() == DamageType.TRIDENT
        )) return;
        if (!(event.getDamageSource().getCausingEntity() instanceof Player)) return;
        if (!event.getEntityType().isAlive()) return;

        Location hitEntityLocation = ((LivingEntity) event.getEntity()).getEyeLocation();
        Optional<LivingEntity> maybeClosestEntity = 
            hitEntityLocation.getNearbyLivingEntities(RADIUS)
                             .stream()
                             .filter((LivingEntity entity) -> !entity.equals(event.getEntity()))
                             .filter((LivingEntity entity) -> !entity.equals(event.getDamageSource().getCausingEntity()))
                             .min(new LivingEntityCloserTo(hitEntityLocation));

        maybeClosestEntity.ifPresent((LivingEntity closestEntity) -> {
            drawParticleLine(hitEntityLocation, closestEntity.getEyeLocation());
            procAbilityDamage(closestEntity, event);
        });
    }

    public void procAbilityDamage(LivingEntity entity, EntityDamageByEntityEvent originalDamageEvent) {
        double finalDamage = originalDamageEvent.getDamage() * SPLIT_ARROW_MULTIPLIER;
        DamageSource cause = 
            DamageSource.builder(DamageType.GENERIC)
                        .withCausingEntity(originalDamageEvent.getDamageSource().getCausingEntity())
                        .withDirectEntity(originalDamageEvent.getDamageSource().getDirectEntity())
                        .build();

        entity.damage(finalDamage, cause);
    }

    public void drawParticleLine(Location a, Location b) {
        Location spawnPosition = a.clone();
        Vector delta = b.clone()
                        .subtract(a)
                        .toVector();
        double maxLength = delta.length();
        Vector normalized_halved = delta.normalize().multiply(0.1);
        for (double interpolate = 0.0; interpolate < maxLength; interpolate += 0.1) {
            spawnPosition.getWorld().spawnParticle(Particle.CRIT, spawnPosition, 2, 0, 0, 0, 0);
            spawnPosition.add(normalized_halved);
        }
    }
}

class LivingEntityCloserTo implements Comparator<LivingEntity> {
    
    Location pointOfComparison;
    
    public LivingEntityCloserTo(Location pointOfComparison) {
        this.pointOfComparison = pointOfComparison;
    }

    public int compare(LivingEntity a, LivingEntity b) {
        double aDist = a.getLocation().distance(pointOfComparison);
        double bDist = b.getLocation().distance(pointOfComparison);
        return Double.compare(aDist, bDist);
    }
}

