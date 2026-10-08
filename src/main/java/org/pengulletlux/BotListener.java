package org.pengulletlux;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.interactions.commands.build.Commands;


public class BotListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Connecté en tant que " + event.getJDA().getSelfUser().getName());
    }

    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                Commands.slash("ping", "Teste la latence du bot, Réponse en: " + event.getJDA().getGatewayPing() + " ms"),
                Commands.slash("message", "Une commande qui teste l'envoie vers le serveur")
        ).queue();
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;           // ⚠️ indispensable : ignorer les bots (dont lui-même)
        String mess = event.getMessage().getContentRaw();
        if (mess.equalsIgnoreCase("lux")) {
            event.getChannel().sendMessage("Pengulet Lux est mon pal favoris ").queue();
        }
        else
        {
            event.getChannel().sendMessage(mess).queue();
        }
    }
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // getName() = le nom de la commande tapée
        switch (event.getName()) {
            case "message": {
                // ↓ event.reply(...) se met ICI : on répond À CETTE interaction
                event.reply("Test de message à envoyer vers Palworld")
                        .setEphemeral(true)
                        .queue();
            }
            case "ping":
            {
                event.reply("Ping de la commande : " + event.getJDA().getGatewayPing() + " ms")
                        .setEphemeral(true)
                        .queue();
            }

            default: event.reply("Commande inconnue").setEphemeral(true).queue();
        }
    }
}
