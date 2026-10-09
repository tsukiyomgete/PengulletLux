package org.pengulletlux;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LecteurChat {

    private String hote;
    private int port;
    private String user;
    private String mdp;
    private long idGuild;

    private transient int lignesDejaLues = 0;   // combien de lignes on a déjà traitées

    public LecteurChat(String hote, int port, String user, String mdp, long idGuild) {
        this.hote = hote;
        this.port = port;
        this.user = user;
        this.mdp = mdp;
        this.idGuild = idGuild;
    }

    private String lireFichier() throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());   // pour tester
        ssh.connect(hote, port);
        ssh.authPassword(user, mdp);

        SFTPClient sftp = ssh.newSFTPClient();
        RemoteFile fichier = sftp.open("/chat.log");
        InputStream flux = fichier.new RemoteFileInputStream();

        String contenu = new String(flux.readAllBytes(), StandardCharsets.UTF_8);

        flux.close();
        fichier.close();
        sftp.close();
        ssh.disconnect();

        return contenu;
    }
    public ArrayList<String> nouvellesLignes() throws IOException {
        String contenu = lireFichier();
        String[] lignes = contenu.split("\n");

        // le fichier a été vidé (redémarrage du serveur) → on repart de 0
        if (lignes.length < lignesDejaLues) {
            lignesDejaLues = 0;
        }

        ArrayList<String> resultat = new ArrayList<>();
        for (int i = lignesDejaLues; i < lignes.length; i++) {
            resultat.add(lignes[i]);
        }

        lignesDejaLues = lignes.length;
        return resultat;
    }
    public long getIdGuild() {
        return idGuild;
    }


}
