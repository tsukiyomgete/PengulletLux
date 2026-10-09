package org.pengulletlux;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StockageSFTP {
    private final Path fichier = Path.of("sftpServeur.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<LecteurChat> loadServList()
    {
        if (!Files.exists(fichier))
        {
            return new ArrayList<>();
        }
        try
        {
            String json = Files.readString(fichier);
            LecteurChat[] tabServ = gson.fromJson(json, LecteurChat[].class);

            if (tabServ == null)
            {
                return new ArrayList<>();
            }
            else
            {
                List<LecteurChat> liste = new ArrayList<>(Arrays.asList(tabServ));
                return liste;
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Erreur de chargement : " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public LecteurChat find(long idGuildServeur)
    {
        List<LecteurChat> liste = loadServList();
        for (int i = 0; i < liste.size(); i++)
        {
            if (liste.get(i).getIdGuild() == idGuildServeur)
            {
                return liste.get(i);
            }
        }
        return null;
    }

    public void save(List<LecteurChat> listServ)
    {
        try {
            String json = gson.toJson(listServ);
            Files.writeString(fichier, json);
        } catch (IOException e)
        {
            System.err.println("Erreur de sauvegarde : " + e.getMessage());
        }
    }
}