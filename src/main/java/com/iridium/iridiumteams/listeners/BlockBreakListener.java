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
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;

@AllArgsConstructor
public class BlockBreakListener<T extends Team, U extends IridiumUser<T>> implements Listener {
    private final IridiumTeams<T, U> iridiumTeams;

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        U user = iridiumTeams.getUserManager().getUser(player);
        Optional<T> teamOptional = iridiumTeams.getTeamManager().getTeamViaLocation(block.getLocation());

        if (teamOptional.isPresent()) {
            T team = teamOptional.get();
            boolean hasBlockBreakPermission = iridiumTeams.getTeamManager().getTeamPermission(team, user, PermissionType.BLOCK_BREAK);
            boolean isCreatureSpawner = block.getState() instanceof CreatureSpawner;
            boolean hasSpawnerPermission = iridiumTeams.getTeamManager().getTeamPermission(team, user, PermissionType.SPAWNERS) || !isCreatureSpawner;
            
            if (!hasBlockBreakPermission || !hasSpawnerPermission) {
                String message = StringUtils.color(iridiumTeams.getMessages().cannotBreakBlocks.replace("%prefix%", iridiumTeams.getConfiguration().prefix));
                player.sendMessage(message);
                event.setCancelled(true);
            }
        } else {
            iridiumTeams.getTeamManager().handleBlockBreakOutsideTerritory(event);
        }

        if (!event.isCancelled()) { 
            handleBlockBreakForMissionsAndCounts(user, block);
        }
    }

    private void handleBlockBreakForMissionsAndCounts(U user, Block block) {
        Bukkit.getScheduler().runTaskAsynchronously(iridiumTeams, () -> {
            XMaterial material = XMaterial.matchXMaterial(block.getType());
            Bukkit.getScheduler().runTask(iridiumTeams, () -> {
                iridiumTeams.getTeamManager().getTeamViaID(user.getTeamID()).ifPresent(team -> {
                    iridiumTeams.getMissionManager().handleMissionUpdate(team, block.getLocation().getWorld(), "MINE", material.name(), 1);
                    
                    TeamBlock teamBlock = iridiumTeams.getTeamManager().getTeamBlock(team, material);
                    teamBlock.setAmount(Math.max(0, teamBlock.getAmount() - 1));

                    if (block.getState() instanceof CreatureSpawner) {
                        CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
                        TeamSpawners teamSpawners = iridiumTeams.getTeamManager().getTeamSpawners(team, creatureSpawner.getSpawnedType());
                        teamSpawners.setAmount(Math.max(0, teamSpawners.getAmount() - 1));
                    }
                });
            });
        });
    }
}
