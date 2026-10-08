package org.pengulletlux;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.entities.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        String discToken = System.getenv("DISCORD_TOKEN");
        JDA jdaBot = JDABuilder.createDefault(discToken)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .disableIntents(GatewayIntent.GUILD_PRESENCES)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Palworld"))
                .addEventListeners(new BotListener())
                .build();
        try{
            jdaBot.awaitReady();
        }catch (InterruptedException e) {
            System.out.println("Méthode jdaBot.awaitRady() interrompu");
        }

    }

}
