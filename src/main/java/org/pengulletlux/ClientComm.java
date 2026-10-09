package org.pengulletlux;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class ClientComm {

    ServeurPalworld servInfo;
    String httpCorps;

    ClientComm(ServeurPalworld sp)
    {
        servInfo = sp;
        httpCorps = "http://" + servInfo.getHostString() + ":" + servInfo.getPort();
    }

    public void sendMessageFromDiscord(String mess)
    {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        String identifiants = servInfo.getAdminUserString() + ":" + servInfo.getAdminPswdString();
        String auth = "Basic " + Base64.getEncoder().encodeToString(identifiants.getBytes(StandardCharsets.UTF_8));

        String corps = new Gson().toJson(Map.of("message", mess));
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(httpCorps+"/v1/api/announce"))
                .header("Content-Type", "application/json")
                .header("Authorization", auth)
                .POST(HttpRequest.BodyPublishers.ofString(corps))
                .build();
        try {
            HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString());

            switch(reponse.statusCode()) {
                case 200 :
                {
                    System.out.println("Succès, le message a été envoyé");
                    break;
                }
                case 401 :
                {
                    System.err.println("Identifiants admin refusé");
                    break;
                }
                default:
                {
                    System.err.println("Erreur HTTP");
                    break;
                }
            }
        } catch (HttpTimeoutException e)
        {
            System.err.println("Le serveur ne répond pas");
        } catch (IOException e)
        {
            System.err.println("Connexion impossible : " + e.getMessage());
        } catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

    }


}
