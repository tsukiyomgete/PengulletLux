package org.pengulletlux;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;


public class BotListener extends ListenerAdapter {

    private List<ServeurPalworld> serveurs = new ArrayList<>();
    private StockageServeur stockage = new StockageServeur();
    private TextChannel salon;
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Connecté en tant que " + event.getJDA().getSelfUser().getName());
        serveurs.addAll(stockage.loadServList());
        System.out.println(serveurs.size() + " serveurs ont été chargés !");
    }

    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                Commands.slash("ping", "Teste la latence du bot, Réponse en: " + event.getJDA().getGatewayPing() + " ms"),
                Commands.slash("message", "Une commande qui envoie un message vers le serveur"),
                Commands.slash("setserveur", "Une commande qui met en place")
                        .addOption(OptionType.STRING, "ip", "Adresse IP du serveur", true)
                        .addOption(OptionType.INTEGER, "port", "Port du serveur (8211 par défaut)",true)
                        .addOption(OptionType.STRING, "adminuser", "Username Admin pour le serveur", true )
                        .addOption(OptionType.STRING, "adminpsw", "pasword Admin pour le serveur", true )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("setchannel", "Mets en place le salon où les messages du serveurs seront reçus et envoyés")
                        .addOption(OptionType.CHANNEL, "channel", "Channel où la discussion entre le serveur Palworld et Discord communiquent", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        ).queue();
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String mess = event.getMessage().getContentRaw();
        if(event.getChannel().equals(salon))
        {
           //envoyer le message sur palworld
            String pseudo = event.getAuthor().getName();
            String messageEnv = "["+pseudo+"]"+": "+ mess;
            System.out.println(messageEnv);
        }
    }



    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "message": {
                event.reply("Test de message à envoyer vers Palworld")
                        .setEphemeral(true)
                        .queue();
                break;
            }
            case "ping":
            {
                event.reply("Ping de la commande : " + event.getJDA().getGatewayPing() + " ms")
                        .setEphemeral(true)
                        .queue();
                break;
            }
            case "setserveur": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue(); return;
                }
                else {
                    String adminUser = event.getOption("adminuser").getAsString();
                    String adminPsw = event.getOption("adminpsw").getAsString();
                    String ip  = event.getOption("ip").getAsString();
                    int port   = event.getOption("port").getAsInt();
                    long idGuild = event.getGuild().getIdLong();
                    serveurs.add(new ServeurPalworld(idGuild,ip,port,adminUser, adminPsw));
                    stockage.save(serveurs);
                    event.reply("le serveur a été mis en place").setEphemeral(true).queue();
                }
                break;
            }
            case "setchannel" : {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue(); return;
                } else {
                    if(stockage.find(event.getGuild().getIdLong())!= null)
                    {
                        salon = event.getChannel().asTextChannel();
                        event.reply("Le salon a été assimilé au serveur").setEphemeral(true).queue();
                    }
                    event.reply("Le serveur n'a pas de salon assimilé").setEphemeral(true).queue();
                }
                break;
            }


            default: {
                event.reply("Commande inconnue").setEphemeral(true).queue();
            }
        }
    }
}
