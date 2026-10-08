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

public class StockageServeur {
    private final Path fichier = Path.of("serveurs.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<ServeurPalworld> loadServList()
    {
        if(!Files.exists(fichier))
        {
            return new ArrayList<>();
        }
        try
        {
            String json = Files.readString(fichier);
            ServeurPalworld[] tabServ = gson.fromJson(json, ServeurPalworld[].class);

            if(tabServ == null)
            {
                return new ArrayList<>();
            }
            else
            {
                List<ServeurPalworld> liste = new ArrayList<>(Arrays.asList(tabServ));
                return liste;
            }
        } catch(IOException e){
            System.err.println("Erreur de chargement :" + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<ServeurPalworld> listServ)
    {
        try {
            String json = gson.toJson(listServ);
            Files.writeString(fichier, json);
        } catch (IOException |JsonSyntaxException e)
        {
            System.err.println("Erreur de sauvegarde : " + e.getMessage());
        }
    }
}
