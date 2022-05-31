package com.iridium.iridiumteams.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.iridium.iridiumcore.utils.StringUtils;
import com.iridium.iridiumteams.TeamBuilder;
import com.iridium.iridiumteams.UserBuilder;
import com.iridium.iridiumteams.gui.ConfirmationGUI;
import com.iridium.testplugin.TestPlugin;
import com.iridium.testplugin.TestTeam;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferCommandTest {

    private ServerMock serverMock;

    @BeforeEach
    public void setup() {
        this.serverMock = MockBukkit.mock();
        MockBukkit.load(TestPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void executeSetHomeCommandBadSyntax() {
        PlayerMock playerMock = new UserBuilder(serverMock).withTeam(new TeamBuilder().build()).build();

        serverMock.dispatchCommand(playerMock, "test transfer");
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getConfiguration().prefix+" &7/team transfer <player>"));
        playerMock.assertNoMoreSaid();
    }

    @Test
    public void executeSetHomeCommandNoFaction() {
        PlayerMock playerMock = new UserBuilder(serverMock).build();
        PlayerMock otherPlayer = new UserBuilder(serverMock).build();

        serverMock.dispatchCommand(playerMock, "test transfer " + otherPlayer.getName());
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().dontHaveTeam.replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)));
        playerMock.assertNoMoreSaid();
    }

    @Test
    public void executeSetHomeCommandNotOwner() {
        TestTeam team = new TeamBuilder().build();
        PlayerMock playerMock = new UserBuilder(serverMock).withTeam(team).build();
        PlayerMock otherPlayer = new UserBuilder(serverMock).build();

        serverMock.dispatchCommand(playerMock, "test transfer " + otherPlayer.getName());
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().mustBeOwnerToTransfer.replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)));
        playerMock.assertNoMoreSaid();
    }

    @Test
    public void executeSetHomeCommandNotValidPlayer() {
        TestTeam team = new TeamBuilder().build();
        PlayerMock playerMock = new UserBuilder(serverMock).withTeam(team).withRank(-1).build();

        serverMock.dispatchCommand(playerMock, "test transfer FakePlayer");
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().notAPlayer.replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)));
        playerMock.assertNoMoreSaid();
    }

    @Test
    public void executeSetHomeCommandPlayerNotInFaction() {
        TestTeam team = new TeamBuilder().build();
        PlayerMock playerMock = new UserBuilder(serverMock).withTeam(team).withRank(-1).build();
        PlayerMock otherPlayer = new UserBuilder(serverMock).build();

        serverMock.dispatchCommand(playerMock, "test transfer " + otherPlayer.getName());
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().userNotInYourTeam.replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)));
        playerMock.assertNoMoreSaid();
    }

    @Test
    public void executeSetHomeCommandCannotTransferYourself() {
        TestTeam team = new TeamBuilder().build();
        PlayerMock playerMock = new UserBuilder(serverMock).withTeam(team).withRank(-1).build();

        serverMock.dispatchCommand(playerMock, "test transfer " + playerMock.getName());
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().cannotTransferToYourself.replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)));
        playerMock.assertNoMoreSaid();
    }

    @Test
    public void executeSetHomeCommandSuccess() {
        TestTeam team = new TeamBuilder().build();
        PlayerMock playerMock = new UserBuilder(serverMock).withTeam(team).withRank(-1).build();
        PlayerMock otherPlayer = new UserBuilder(serverMock).withTeam(team).build();

        serverMock.dispatchCommand(playerMock, "test transfer " + otherPlayer.getName());
        assertTrue(playerMock.getOpenInventory().getTopInventory().getHolder() instanceof ConfirmationGUI);
        ConfirmationGUI<?, ?> confirmationGUI = (ConfirmationGUI<?, ?>) playerMock.getOpenInventory().getTopInventory().getHolder();
        confirmationGUI.onInventoryClick(new InventoryClickEvent(playerMock.getOpenInventory(), InventoryType.SlotType.CONTAINER, TestPlugin.getInstance().getInventories().confirmationGUI.yes.slot, ClickType.LEFT, InventoryAction.UNKNOWN));

        assertEquals(3, TestPlugin.getInstance().getUserManager().getUser(playerMock).getUserRank());
        assertEquals(-1, TestPlugin.getInstance().getUserManager().getUser(otherPlayer).getUserRank());
        playerMock.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().ownershipTransferred
                .replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)
                .replace("%old_owner%", playerMock.getName())
                .replace("%new_owner%", otherPlayer.getName())
        ));
        playerMock.assertNoMoreSaid();
        otherPlayer.assertSaid(StringUtils.color(TestPlugin.getInstance().getMessages().ownershipTransferred
                .replace("%prefix%", TestPlugin.getInstance().getConfiguration().prefix)
                .replace("%old_owner%", playerMock.getName())
                .replace("%new_owner%", otherPlayer.getName())
        ));
        otherPlayer.assertNoMoreSaid();
    }
}