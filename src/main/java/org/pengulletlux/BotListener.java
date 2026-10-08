package org.pengulletlux;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;


public class BotListener extends ListenerAdapter {

    private List<ServeurPalworld> listServ = new ArrayList<>();
    private StockageServeur stockage = new StockageServeur();

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Connecté en tant que " + event.getJDA().getSelfUser().getName());
    }

    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                Commands.slash("ping", "Teste la latence du bot, Réponse en: " + event.getJDA().getGatewayPing() + " ms"),
                Commands.slash("message", "Une commande qui envoie un message vers le serveur"),
                Commands.slash("setserveur", "Une commande qui met en place")
                        .addOption(OptionType.STRING, "ip", "Adresse IP du serveur", true)
                        .addOption(OptionType.INTEGER, "port", "Port du serveur (8211 par défaut)",true)
                        .addOption(OptionType.STRING, "adminUser", "Username Admin pour le serveur", true )
                        .addOption(OptionType.STRING, "adminPsw", "pasword Admin pour le serveur", true )
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
            case "setserveur": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue(); return;
                }
                else {
                    String adminUser = event.getOption("adminUser").getAsString();
                    String adminPsw = event.getOption("adminPsw").getAsString();
                    String ip  = event.getOption("ip").getAsString();
                    int port   = event.getOption("port").getAsInt();
                    long idGuild = event.getGuild().getIdLong();
                    listServ.add(new ServeurPalworld(idGuild,ip,port,adminUser, adminPsw));
                }
            }

            default: event.reply("Commande inconnue").setEphemeral(true).queue();
        }
    }
}
