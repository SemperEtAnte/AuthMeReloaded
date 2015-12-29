package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.crypts.HashResult;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Admin command for changing a player's password.
 */
public class ChangePasswordAdminCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player and password
        String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);

        // Validate the password
        String playerPassLowerCase = playerPass.toLowerCase();
        // TODO #308: Remove this check
        if (playerPassLowerCase.contains("delete") || playerPassLowerCase.contains("where")
            || playerPassLowerCase.contains("insert") || playerPassLowerCase.contains("modify")
            || playerPassLowerCase.contains("from") || playerPassLowerCase.contains("select")
            || playerPassLowerCase.contains(";") || playerPassLowerCase.contains("null")
            || !playerPassLowerCase.matches(Settings.getPassRegex)) {
            commandService.send(sender, MessageKey.PASSWORD_MATCH_ERROR);
            return;
        }
        if (playerPassLowerCase.equalsIgnoreCase(playerName)) {
            commandService.send(sender, MessageKey.PASSWORD_IS_USERNAME_ERROR);
            return;
        }
        if (playerPassLowerCase.length() < Settings.getPasswordMinLen
                || playerPassLowerCase.length() > Settings.passwordMaxLength) {
            commandService.send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
            return;
        }
        if (!Settings.unsafePasswords.isEmpty() && Settings.unsafePasswords.contains(playerPassLowerCase)) {
            commandService.send(sender, MessageKey.PASSWORD_UNSAFE_ERROR);
            return;
        }
        // Set the password
        final String playerNameLowerCase = playerName.toLowerCase();
        commandService.runTaskAsynchronously(new Runnable() {

            @Override
            public void run() {
                DataSource dataSource = commandService.getDataSource();
                PlayerAuth auth = null;
                if (PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
                    auth = PlayerCache.getInstance().getAuth(playerNameLowerCase);
                } else if (dataSource.isAuthAvailable(playerNameLowerCase)) {
                    auth = dataSource.getAuth(playerNameLowerCase);
                }
                if (auth == null) {
                    commandService.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                }

                // TODO #358: Do we always pass lowercase name?? In that case we need to do that in PasswordSecurity!
                HashResult hashResult = commandService.getPasswordSecurity().computeHash(playerPass, playerNameLowerCase);
                auth.setHash(hashResult.getHash());
                auth.setSalt(hashResult.getSalt());

                // TODO #358: updatePassword(auth) needs to update the salt, too.
                if (!dataSource.updatePassword(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                } else {
                    commandService.send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
                    ConsoleLogger.info(playerNameLowerCase + "'s password changed");
                }
            }

        });
    }
}
