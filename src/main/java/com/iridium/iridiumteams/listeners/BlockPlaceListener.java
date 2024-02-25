package com.iridium.iridiumteams.listeners;

import com.iridium.iridiumcore.dependencies.xseries.XMaterial;
import com.iridium.iridiumcore.utils.StringUtils;
import com.iridium.iridiumteams.IridiumTeams;
import com.iridium.iridiumteams.PermissionType;
import com.iridium.iridiumteams.database.IridiumUser;
import com.iridium.iridiumteams.database.Team;
import com.iridium.iridiumteams.database.TeamBlock;
import com.iridium.iridiumteams.database.TeamSpawners;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

@AllArgsConstructor
public class BlockPlaceListener<T extends Team, U extends IridiumUser<T>> implements Listener {
    private final IridiumTeams<T, U> iridiumTeams;
    private final String cannotPlaceBlocksMessage;

    public BlockPlaceListener(IridiumTeams<T, U> iridiumTeams) {
        this.iridiumTeams = iridiumTeams;
        this.cannotPlaceBlocksMessage = StringUtils.color(iridiumTeams.getMessages().cannotPlaceBlocks
                .replace("%prefix%", iridiumTeams.getConfiguration().prefix));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (iridiumTeams.getTeamManager().isBankItem(event.getItemInHand())) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        U user = iridiumTeams.getUserManager().getUser(player);
        Location blockLocation = event.getBlock().getLocation();
        Optional<T> teamOpt = iridiumTeams.getTeamManager().getTeamViaLocation(blockLocation);

        teamOpt.ifPresentOrElse(team -> {
            if (!iridiumTeams.getTeamManager().getTeamPermission(team, user, PermissionType.BLOCK_PLACE)) {
                player.sendMessage(cannotPlaceBlocksMessage);
                event.setCancelled(true);
            }
        }, () -> iridiumTeams.getTeamManager().handleBlockPlaceOutsideTerritory(event));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void monitorBlockPlace(BlockPlaceEvent event) {
        U user = iridiumTeams.getUserManager().getUser(event.getPlayer());
        Material blockType = event.getBlock().getType();
        XMaterial material = XMaterial.matchXMaterial(blockType);
        Location blockLocation = event.getBlock().getLocation();
        World world = blockLocation.getWorld();

        iridiumTeams.getTeamManager().getTeamViaID(user.getTeamID())
                .ifPresent(team -> iridiumTeams.getMissionManager().handleMissionUpdate(team, world, "PLACE", material.name(), 1));

        iridiumTeams.getTeamManager().getTeamViaLocation(blockLocation).ifPresent(team -> {
            TeamBlock teamBlock = iridiumTeams.getTeamManager().getTeamBlock(team, material);
            teamBlock.setAmount(teamBlock.getAmount() + 1);

            if (blockType == Material.SPAWNER) {
                CreatureSpawner creatureSpawner = (CreatureSpawner) event.getBlock().getState();
                TeamSpawners teamSpawners = iridiumTeams.getTeamManager().getTeamSpawners(team, creatureSpawner.getSpawnedType());
                teamSpawners.setAmount(teamSpawners.getAmount() + 1);
            }
        });
    }
}

