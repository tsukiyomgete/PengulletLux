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
    private ServeurPalworld servP;

    public BotListener() {
        serveurs.addAll(stockage.loadServList());
        System.out.println(serveurs.size() + " serveurs ont été chargés !");
    }

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
                        .addOption(OptionType.STRING, "adminuser", "Username Admin pour le serveur", true )
                        .addOption(OptionType.STRING, "adminpsw", "pasword Admin pour le serveur", true )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("setchannel", "Mets en place le salon où les messages du serveurs seront reçus et envoyés")
                        .addOption(OptionType.CHANNEL, "channel", "Channel où la discussion entre le serveur Palworld et Discord communiquent", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        ).queue();
        // [MODIF] les 2 lignes de chargement ont été retirées d'ici (déplacées dans le constructeur)
        for (ServeurPalworld s : serveurs) {
            if (s.getIdGuild() == event.getGuild().getIdLong()) {
                servP = s;
                break;
            }
        }
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String mess = event.getMessage().getContentRaw();

        if(servP != null)
        {
            if(event.getChannel().getIdLong() == servP.getIdChannel())
            {
                ClientComm comm = new ClientComm(servP);
                String pseudo = event.getAuthor().getName();
                String messageEnv = "["+pseudo+"]"+": "+ mess;
                System.out.println(messageEnv);
                comm.sendMessageFromDiscord(messageEnv);
            }
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
                    serveurs.removeIf(sp -> sp.getIdGuild() == idGuild);   // [MODIF] évite les doublons
                    serveurs.add(new ServeurPalworld(idGuild,ip,port,adminUser, adminPsw));
                    stockage.save(serveurs);
                    event.reply("le serveur a été mis en place").setEphemeral(true).queue();
                }
                break;
            }
            case "setchannel": {
                long idGuild = event.getGuild().getIdLong();


                servP = null;
                for (ServeurPalworld s : serveurs) {
                    if (s.getIdGuild() == idGuild) {
                        servP = s;
                        break;
                    }
                }

                if(servP == null)
                {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }


                TextChannel salon = event.getGuild().getTextChannelById(event.getChannel().getIdLong());
                if (salon == null) {
                    event.reply("Salon introuvable, refais /setchannel").setEphemeral(true).queue();
                    return;
                }
                servP.setChannelText(salon.getIdLong());
                stockage.save(serveurs);
                event.reply("Ce salon est maintenant le salon du bot ").setEphemeral(true).queue();
                break;
            }
            default: {
                event.reply("Commande inconnue").setEphemeral(true).queue();
            }
        }
    }
}