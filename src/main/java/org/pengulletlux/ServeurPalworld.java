package org.pengulletlux;

public class ServeurPalworld {
    private long idGuild;
    private String host;
    private int port;
    private String adminUser;
    private String adminPass;
    private long idChannelText;

    ServeurPalworld(long idGuild,String host, int port, String adminUser, String adminPass)
    {
        this.idGuild = idGuild;
        this.host = host;
        this.port = port;
        this.adminUser = adminUser;
        this.adminPass = adminPass;
    }

    public void setChannelText(long id)
    {
        idChannelText = id;
    }

    public long getIdChannel()
    {
        return idChannelText;
    }

    public long getIdGuild()
    {
        return idGuild;
    }
    public String getHostString()
    {
        return host;
    }
    public String getAdminUserString()
    {
        return adminUser;
    }
    public String getAdminPswdString()
    {
        return adminPass;
    }
    public int getPort()
    {
        return port;
    }
}
