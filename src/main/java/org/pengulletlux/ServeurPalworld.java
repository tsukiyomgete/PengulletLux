package org.pengulletlux;

public class ServeurPalworld {
    private String host;
    private String port;
    private String adminUser;
    private String adminPass;

    ServeurPalworld(String host, String port, String adminUser, String adminPass)
    {
        this.host = host;
        this.port = port;
        this.adminUser = adminUser;
        this.adminPass = adminPass;
    }
}
