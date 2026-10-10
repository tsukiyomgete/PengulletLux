package org.pengulletlux;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;   // [MODIF] seulement Color (java.awt.* contient aussi un "List")
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class BotListener extends ListenerAdapter {


    private final List<ServeurPalworld> serveurs = new CopyOnWriteArrayList<>();
    private final StockageServeur stockage = new StockageServeur();
    private final List<LecteurChat> lecteurs = new CopyOnWriteArrayList<>();
    private final StockageSFTP stockageSftp = new StockageSFTP();


    public BotListener() {
        serveurs.addAll(stockage.loadServList());
        System.out.println(serveurs.size() + " serveurs ont été chargés !");

        lecteurs.addAll(stockageSftp.loadServList());
        System.out.println(lecteurs.size() + " config(s) SFTP chargée(s) !");
    }


    private ServeurPalworld trouverServeur(long idGuild) {
        for (int i = 0; i < serveurs.size(); i++) {
            if (serveurs.get(i).getIdGuild() == idGuild) {
                return serveurs.get(i);
            }
        }
        return null;
    }


    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Connecté en tant que " + event.getJDA().getSelfUser().getName());
        JDA jda = event.getJDA();

        Thread threadLecture = new Thread(() -> {
            while (true) {
                for (int i = 0; i < lecteurs.size(); i++) {
                    LecteurChat lecteur = lecteurs.get(i);
                    try {
                        ArrayList<String> lignes = lecteur.nouvellesLignes();

                        ServeurPalworld sp = trouverServeur(lecteur.getIdGuild());
                        if (sp == null) continue;

                        TextChannel salon = jda.getTextChannelById(sp.getIdChannel());
                        if (salon == null) continue;

                        for (int j = 0; j < lignes.size(); j++) {
                            if (!lignes.get(j).isBlank()) {
                                String mess = lignes.get(j);
                                String[] morceaux = mess.split(" ");

                                for (int k = 0; k < morceaux.length; k++) {
                                    if (morceaux[k].equals("[CHAT]") && k + 1 < morceaux.length) {
                                        String pseudo = morceaux[k + 1].replace("<", "").replace(">", "");

                                        String message = "";
                                        for (int m = k + 2; m < morceaux.length; m++) {
                                            message = message + morceaux[m] + " ";
                                        }

                                        EmbedBuilder eb = new EmbedBuilder();
                                        eb.setAuthor(pseudo);                 // le pseudo en haut, en petit
                                        eb.setDescription(message);           // le message
                                        eb.setColor(Color.BLUE);              // la barre de couleur à gauche
                                        salon.sendMessageEmbeds(eb.build()).queue();
                                    }

                                    if (morceaux[k].equals("[LOG]") && k + 2 < morceaux.length) {
                                        String pseudo = morceaux[k + 1];
                                        String action = morceaux[k + 2];

                                        if (action.equals("joined")) {
                                            EmbedBuilder eb = new EmbedBuilder();
                                            eb.setDescription("🟢 **" + pseudo + "** a rejoint le serveur");
                                            eb.setColor(Color.GREEN);
                                            salon.sendMessageEmbeds(eb.build()).queue();
                                        } else if (action.equals("left")) {
                                            EmbedBuilder eb = new EmbedBuilder();
                                            eb.setDescription("🔴 **" + pseudo + "** a quitté le serveur");
                                            eb.setColor(Color.RED);
                                            salon.sendMessageEmbeds(eb.build()).queue();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur SFTP : " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Erreur lecture chat : " + e.getMessage());
                    }
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });

        threadLecture.setDaemon(true);   // s'arrête avec le bot
        threadLecture.start();
    }


    @Override // [MODIF] ajouté
    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                // [MODIF] description fixe (le ping calculé ici restait figé)
                Commands.slash("ping", "Teste la latence du bot"),
                Commands.slash("message", "Une commande qui envoie un message vers le serveur"),
                Commands.slash("setserveur", "Configure l'API REST du serveur Palworld")
                        .addOption(OptionType.STRING, "ip", "Adresse IP du serveur", true)
                        // [MODIF] description : c'est le port de l'API REST, pas celui du jeu
                        .addOption(OptionType.INTEGER, "port", "Port de l'API REST (RESTAPIPort)", true)
                        .addOption(OptionType.STRING, "adminuser", "Username Admin pour le serveur (admin)", true)
                        .addOption(OptionType.STRING, "adminpsw", "Password Admin pour le serveur", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("setchannel", "Met en place le salon où les messages du serveur seront reçus et envoyés")
                        .addOption(OptionType.CHANNEL, "channel", "Salon de discussion entre le serveur Palworld et Discord", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("setsftp", "Configure l'accès SFTP pour lire le chat du jeu")
                        .addOption(OptionType.STRING, "hote", "Adresse SFTP (ex : gm02.inovaperf.fr, sans sftp://)", true)
                        .addOption(OptionType.INTEGER, "port", "Port SFTP (ex : 2022)", true)
                        .addOption(OptionType.STRING, "user", "Utilisateur SFTP", true)
                        .addOption(OptionType.STRING, "password", "Mot de passe SFTP", true)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("serverinfo", "Affiche les infos du serveur Palworld"),
                Commands.slash("kick", "Expulse un joueur du serveur")
                        // [MODIF] STRING au lieu de INTEGER : l'ID Palworld ressemble à "steam_7656..."
                        .addOption(OptionType.STRING, "idsteam", "ID du joueur (ex : steam_7656...)", true)
                        .addOption(OptionType.STRING, "reason", "Raison du kick", false)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                // [MODIF] /ban n'était pas déclarée
                Commands.slash("ban", "Bannit un joueur du serveur")
                        .addOption(OptionType.STRING, "idsteam", "ID du joueur (ex : steam_7656...)", true)
                        .addOption(OptionType.STRING, "reason", "Raison du ban", false)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("save", "Sauvegarde le serveur")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("shutdown", "Ferme le serveur avec un compte à rebours")
                        .addOption(OptionType.INTEGER, "waittime", "Nombre de secondes avant la fermeture", true)
                        .addOption(OptionType.STRING, "message", "Message affiché aux joueurs", true)
                        // [MODIF] sans ça, n'importe quel membre pouvait éteindre le serveur
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                // [MODIF] /forcestop n'était pas déclarée
                Commands.slash("forcestop", "Arrête le serveur immédiatement (sans sauvegarde)")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("adminplayerlist", "Recevoir la liste (version admin)")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("playerlist", "Recevoir la liste des joueurs"),
                Commands.slash("ipserveur", "renvoie l'ip du serveur pour se connecter")
        ).queue();
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
        if (sp == null) return;

        if (event.getChannel().getIdLong() == sp.getIdChannel()) {
            String mess = event.getMessage().getContentRaw();
            String pseudo = event.getAuthor().getName();
            String messageEnv = "[" + pseudo + "]: " + mess;
            System.out.println(messageEnv);

            ClientComm comm = new ClientComm(sp);
            comm.sendMessageFromDiscord(messageEnv);
        }
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "message": {
                event.reply("Test de message à envoyer vers Palworld")
                        .setEphemeral(true)
                        .queue();
                break;
            }
            case "ping": {
                event.reply("Ping de la commande : " + event.getJDA().getGatewayPing() + " ms")
                        .setEphemeral(true)
                        .queue();
                break;
            }
            case "setserveur": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                String adminUser = event.getOption("adminuser").getAsString();
                String adminPsw = event.getOption("adminpsw").getAsString();
                String ip = event.getOption("ip").getAsString();
                int port = event.getOption("port").getAsInt();
                long idGuild = event.getGuild().getIdLong();

                ServeurPalworld ancien = trouverServeur(idGuild);

                serveurs.removeIf(sp -> sp.getIdGuild() == idGuild);
                ServeurPalworld nouveau = new ServeurPalworld(idGuild, ip, port, adminUser, adminPsw);
                if (ancien != null) {
                    nouveau.setChannelText(ancien.getIdChannel());
                }
                serveurs.add(nouveau);
                stockage.save(serveurs);

                event.reply("Le serveur a été mis en place ✅").setEphemeral(true).queue();
                break;
            }
            case "setsftp": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                String hote = event.getOption("hote").getAsString();
                int port = event.getOption("port").getAsInt();
                String user = event.getOption("user").getAsString();
                String mdp = event.getOption("password").getAsString();   // [MODIF] "password" = nom déclaré
                long idGuild = event.getGuild().getIdLong();

                lecteurs.removeIf(l -> l.getIdGuild() == idGuild);
                lecteurs.add(new LecteurChat(hote, port, user, mdp, idGuild));
                stockageSftp.save(lecteurs);

                event.reply("La configuration SFTP a été enregistrée ✅").setEphemeral(true).queue();
                break;
            }
            case "setchannel": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }

                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                long idSalon = event.getOption("channel").getAsChannel().getIdLong();
                TextChannel salon = event.getGuild().getTextChannelById(idSalon);
                if (salon == null) {
                    event.reply("Choisis un salon textuel").setEphemeral(true).queue();
                    break;
                }

                sp.setChannelText(salon.getIdLong());
                stockage.save(serveurs);
                event.reply("Le salon " + salon.getAsMention() + " est maintenant le salon du bot ✅")
                        .setEphemeral(true).queue();
                break;
            }
            case "serverinfo": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();   // [MODIF] on réserve la réponse AVANT les appels à l'API

                ClientComm comm = new ClientComm(sp);
                String servInfoJson = comm.getServerInfo();
                String servMetricJson = comm.getServerMetrics();
                if (servInfoJson == null || servMetricJson == null) {   // [MODIF] serveur injoignable
                    event.getHook().sendMessage("Impossible de joindre le serveur Palworld 😕").queue();
                    break;
                }

                JsonObject jsonInfo = JsonParser.parseString(servInfoJson).getAsJsonObject();
                JsonObject jsonMetrics = JsonParser.parseString(servMetricJson).getAsJsonObject();
                int fps = jsonMetrics.get("serverfps").getAsInt();
                int maxJoueur = jsonMetrics.get("maxplayernum").getAsInt();
                int currentJoueur = jsonMetrics.get("currentplayernum").getAsInt();
                double frameTime = jsonMetrics.get("serverframetime").getAsDouble();
                int basecampnum = jsonMetrics.get("basecampnum").getAsInt();
                int days = jsonMetrics.get("days").getAsInt();

                String servername = jsonInfo.get("servername").getAsString();
                String description = jsonInfo.get("description").getAsString();
                String version = jsonInfo.get("version").getAsString();

                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Serveur Palworld")
                        .addField("Nom : ", servername, false)
                        .addField("Description : ", description, false)
                        .addField("Version : ", version, true)
                        .addField("Nombre Joueur : ", currentJoueur + "/" + maxJoueur, true)
                        .addField("Server Frame Time: ", frameTime + "", true)
                        .addField("FPS : ", fps + "", true)
                        .addField("Nombre de base : ", basecampnum + "", false)
                        .addField("Jour n° ", days + "", true)
                        .setColor(Color.YELLOW);
                event.getHook().sendMessageEmbeds(eb.build()).queue();   // [MODIF] réponse via le hook
                break;
            }
            case "kick": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();   // [MODIF] déplacé ici : après les vérifs qui utilisent event.reply

                String id = event.getOption("idsteam").getAsString();   // [MODIF] "idsteam" = nom déclaré
                String raison = "Aucune raison";                        // [MODIF] option facultative
                if (event.getOption("reason") != null) {
                    raison = event.getOption("reason").getAsString();
                }

                ClientComm comm = new ClientComm(sp);
                comm.kickPlayerId(raison, id);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setDescription("**" + id + "** a été kick du serveur !");
                eb.addField("Raison :", raison, false);
                eb.setColor(Color.ORANGE);
                event.getHook().sendMessageEmbeds(eb.build()).queue();   // [MODIF]
                break;
            }
            case "ban": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();   // [MODIF]

                String id = event.getOption("idsteam").getAsString();   // [MODIF]
                String raison = "Aucune raison";                        // [MODIF]
                if (event.getOption("reason") != null) {
                    raison = event.getOption("reason").getAsString();
                }

                ClientComm comm = new ClientComm(sp);
                comm.banPlayerId(raison, id);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setDescription("**" + id + "** a été banni du serveur !");
                eb.addField("Raison :", raison, false);
                eb.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(eb.build()).queue();   // [MODIF]
                break;
            }
            case "save": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();

                ClientComm comm = new ClientComm(sp);
                comm.saveServer();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setDescription("Le serveur a été sauvegardé !");
                eb.setColor(Color.GREEN);
                event.getHook().sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "shutdown": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();

                int attente = event.getOption("waittime").getAsInt();
                String message = event.getOption("message").getAsString();

                ClientComm comm = new ClientComm(sp);
                comm.shutdownServer(message, attente);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setDescription("Le serveur va fermer dans " + attente + " secondes");
                eb.setColor(Color.ORANGE);
                event.getHook().sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "forcestop": {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();

                ClientComm comm = new ClientComm(sp);
                comm.forceStop();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setDescription("Le serveur a été arrêté de force.");
                eb.setColor(Color.RED);
                event.getHook().sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "playerlist":
            {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }
                event.deferReply().queue();
                ClientComm comm = new ClientComm(sp);
                String json = comm.getPlayerList();
                if (json == "") {
                    event.getHook().sendMessage("Impossible de joindre le serveur Palworld").queue();
                    break;
                }
                JsonArray joueurs = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("players");
                String liste = "";
                for (int i = 0; i < joueurs.size(); i++) {
                    JsonObject joueur = joueurs.get(i).getAsJsonObject();
                    String nom = joueur.get("name").getAsString();
                    int niveau = joueur.get("level").getAsInt();
                    double ping = joueur.get("ping").getAsDouble();
                    liste = liste + "**" + nom + "** (niv. " + niveau + ") latence : " + ping + "\n" ;
                }
                if (joueurs.size() == 0) {
                    liste = "Aucun joueur connecté";
                }
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Joueurs connectés (" + joueurs.size() + ")")
                        .setDescription(liste)
                        .setColor(Color.CYAN);
                event.getHook().sendMessageEmbeds(eb.build()).queue();
                break;
            }
            case "ipserveur":
            {
                if (!event.isFromGuild()) {
                    event.reply("Commande utilisable uniquement sur un serveur").setEphemeral(true).queue();
                    return;
                }
                ServeurPalworld sp = trouverServeur(event.getGuild().getIdLong());
                if (sp == null) {
                    event.reply("Configure d'abord le serveur avec /setserveur").setEphemeral(true).queue();
                    break;
                }
                event.deferReply().queue();
                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("IP du serveur : " + sp.getHostString()+ ":"+ sp.getPort())
                        .setColor(Color.YELLOW);
                event.getHook().sendMessageEmbeds(eb.build()).queue();

            }

            default: {
                event.reply("Commande inconnue").setEphemeral(true).queue();
            }
        }
    }
}