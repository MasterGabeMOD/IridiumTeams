package com.iridium.iridiumteams.listeners;

import com.iridium.iridiumcore.utils.StringUtils;
import com.iridium.iridiumteams.IridiumTeams;
import com.iridium.iridiumteams.database.IridiumUser;
import com.iridium.iridiumteams.database.Team;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

@AllArgsConstructor
public class PlayerTeleportListener<T extends Team, U extends IridiumUser<T>> implements Listener {
    private final IridiumTeams<T, U> iridiumTeams;

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) return; // This is possible apparently?
        U user = iridiumTeams.getUserManager().getUser(player);
        Optional<T> toTeam = iridiumTeams.getTeamManager().getTeamViaLocation(to);
        if (user.isFlying() && (to.getBlockX() != from.getBlockX() || to.getBlockZ() != from.getBlockZ()) && !user.canFly(iridiumTeams)) {
            user.setFlying(false);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(StringUtils.color(iridiumTeams.getMessages().flightDisabled
                    .replace("%prefix%", iridiumTeams.getConfiguration().prefix))
            );
        }
        if (toTeam.isPresent() && !iridiumTeams.getTeamManager().canVisit(player, toTeam.get())) {
            event.setCancelled(true);
            player.sendMessage(StringUtils.color(iridiumTeams.getMessages().cannotVisit
                    .replace("%prefix%", iridiumTeams.getConfiguration().prefix))
            );
        }
    }

}
