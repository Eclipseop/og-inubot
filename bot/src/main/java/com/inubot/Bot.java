/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the license, or (at your option) any later version.
 */
package com.inubot;

import com.inubot.api.event.EventBus;
import com.inubot.api.event.impl.AsynchronousEventBus;
import com.inubot.api.event.impl.SynchronousEventBus;
import com.inubot.api.methods.Game;
import com.inubot.api.util.Time;
import com.inubot.bot.account.Account;
import com.inubot.bot.account.AccountManager;
import com.inubot.bot.modscript.Injector;
import com.inubot.bot.modscript.ModScript;
import com.inubot.bot.ui.BotMenuBar;
import com.inubot.bot.util.*;
import com.inubot.bot.util.io.Crawler;
import com.inubot.client.GameCanvas;
import com.inubot.client.natives.ClientNative;
import com.inubot.script.ScriptFlux;

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class Bot<Client extends ClientNative> extends JFrame implements Runnable {

    private static final String IRC_CHANNEL = "#inubot";
    protected static Bot instance;
    private static boolean useProxy = false;
    protected final Crawler crawler;
    private final ScriptFlux scriptFlux;
    private EventBus asyncEventBus;
    private EventBus syncEventBus;
    private Client client;

    private TrayIcon tray = null;

    public Bot() {
        super(Configuration.APPLICATION_NAME);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        setBackground(Color.BLACK);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Image image16 = toolkit.getImage(Bot.class.getClassLoader().getResource("icon16.png"));
        Image image32 = toolkit.getImage(Bot.class.getClassLoader().getResource("icon32.png"));
        Image image64 = toolkit.getImage(Bot.class.getClassLoader().getResource("icon64.png"));
        Image image128 = toolkit.getImage(Bot.class.getClassLoader().getResource("icon128.png"));
        setIconImages(Arrays.asList(image16, image32, image64, image128));

        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            this.tray = new TrayIcon(image16, "Inubot");
            this.tray.setImageAutoSize(true);
            try {
                tray.add(this.tray);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }

        this.scriptFlux = new ScriptFlux();
        this.crawler = createCrawler();
        this.asyncEventBus = new AsynchronousEventBus();
        this.syncEventBus = new SynchronousEventBus();
       // ServerConnection.start();
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        new Thread(this).start();
    }

    public static <T extends ClientNative> Bot<T> getInstance() {
        return instance;
    }

    @Override
    public void run() {
        BotMenuBar menuBar = new BotMenuBar();
        super.setJMenuBar(menuBar);
        crawler.crawl();
        boolean forceInject;
        if (forceInject = crawler.isOutdated())
            crawler.download();
        try {
            byte[] modscript = Files.readAllBytes(Paths.get(Configuration.CACHE + "oldschool.dat"));
            //byte[] modscript = Internet.downloadBinary(new URL(crawler.modscript).openStream(), null)
            ModScript.load(modscript, Integer.toString(crawler.getHash()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse modscript", e);
        }

        deleteRandom();
        File pack = new File(crawler.pack);
        File injectCache = new File(Configuration.INJECT_CACHE);
        int hash = -1;
        try (JarInputStream stream = new JarInputStream(new FileInputStream(Configuration.INJECTED))) {
            hash = stream.getManifest().hashCode();
        } catch (Exception ignore) {

        }
        boolean inject = true;
        try {
            if ((!injectCache.exists() && new File(Configuration.INJECTED).exists())) {
                injectCache.createNewFile();
                BufferedWriter fw = new BufferedWriter(new FileWriter(injectCache));
                fw.write(String.valueOf(hash));
                fw.close();
            } else if (injectCache.exists()) {
                BufferedReader fr = new BufferedReader(new FileReader(injectCache));
                int value = fr.read();
                inject = value == hash;
                fr.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, byte[]> classes = new HashMap<>();
        if (inject || forceInject || !new File(Configuration.INJECTED).exists()) {
            Injector injector = initInjector(pack);
            classes = injector.inject(true);
        } else {
            try (ZipInputStream input = new JarInputStream(new FileInputStream(Configuration.INJECTED))) {
                ZipEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (entryName.endsWith(".class")) {
                        byte[] read = read(input);
                        entryName = entryName.replace('/', '.');
                        String name = entryName.substring(0, entryName.length() - 6);
                        classes.put(name, read);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        CachedClassLoader classloader = new CachedClassLoader(classes);
        ModScript.setClassLoader(classloader);

        if (useProxy && ProxyUtils.useAliveUSProxy())
            setTitle(Configuration.APPLICATION_NAME + " - Proxy [" + ProxyUtils.getLastIP() + ":" + ProxyUtils.getLastPort() + "]");

        setSize(780, 565);
        setLocationRelativeTo(null);
        setVisible(true);

        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.setBackground(Color.BLACK);

        this.client = (Client) crawler.start(classloader);
        container.add(((Applet) client), BorderLayout.CENTER);
        pack();

        if (instance == null) {
            instance = this;
        }

        while (Game.getState() < Game.STATE_CREDENTIALS_SCREEN || ((Applet) client).getComponentCount() == 0) {
            Time.sleep(100);
        }

        initCache(client);

        getCanvas().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!e.isControlDown())
                    return;
                for (Hotkey cb : Hotkey.values()) {
                    if (e.getKeyCode() == cb.getKey()) {
                        cb.onActivation();
                    }
                }
            }
        });
        if (instance == null) {
            instance = this;
        }
    }

    private void deleteRandom() {
        File f = new File(System.getProperty("user.home"),"random.dat");
        if(f.exists()) {
            try {
                f.setWritable(true);
                RandomAccessFile raf = new RandomAccessFile(f,"rw");
                {
                    byte[] bytes = new byte[24];
                    raf.seek(0);
                    raf.read(bytes);
                    CRC32 crc = new CRC32();
                    crc.update(bytes);
                    long val = crc.getValue();
                    System.out.println("Random:" + val + ":" + Arrays.toString(bytes));
                }
                {
                    raf.seek(0);
                    raf.write(new byte[24]);
                    raf.getChannel().force(true);
                    raf.close();
                    System.out.println("KILLED RANDOM.DAT");
                }
                if (!f.setReadOnly()) {
                    System.out.println("FAILED TO SET RANDOM.DAT READ ONLY");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //We will ensure random.dat is created under user.home and does not end up somewhere else....
            System.out.println("No random.dat");
            try {
                if(!f.createNewFile()) {
                    throw new RuntimeException();
                }
            } catch (IOException ignored) {
                throw new RuntimeException("Failed to generate random.dat",ignored);
            }
        }
    }

    public Client getClient() {
        return client;
    }

    public GameCanvas getCanvas() {
        return ((Applet) client).getComponentCount() > 0 ? (GameCanvas) ((Applet) client).getComponent(0) : null;
    }

    public void sendNotification(String title, String message) {
        tray.displayMessage(title, message, TrayIcon.MessageType.INFO);
    }

    @Override
    public BotMenuBar getJMenuBar() {
        return (BotMenuBar) super.getJMenuBar();
    }

    public EventBus getSyncEventBus() {
        return syncEventBus;
    }

    public void setSyncEventBus(EventBus eventBus) {
        this.syncEventBus = eventBus;
    }

    public EventBus getAsyncEventBus() {
        return asyncEventBus;
    }

    public void setAsyncEventBus(EventBus eventBus) {
        this.asyncEventBus = eventBus;
    }

    public ScriptFlux getScriptFlux() {
        return scriptFlux;
    }

    protected abstract Injector initInjector(File pack);

    protected abstract void initCache(Client client);

    protected abstract Crawler createCrawler();

    protected void initArgs(String... args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-proxy")) {
                String username = args[i + 1];
                String password = args[i + 2];
                String address = args[i + 3];
                String port = args[i + 4];

                Authenticator authenticator = new Authenticator() {

                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                };
                System.setProperty("java.net.socks.username", username);
                System.setProperty("java.net.socks.password", password);
                System.setProperty("socksProxyHost", address);
                System.setProperty("socksProxyPort", port);
                java.net.Authenticator.setDefault(authenticator);
            }
            if (arg.equals("-account")) {
                String user = args[i + 1];
                String pass = args[i + 2];
                AccountManager.setCurrentAccount(new Account(user, pass));
                System.out.println("Account is now " + user + " : " + pass);
            }
            if (arg.equals("-world")) {
                Configuration.WORLD = Integer.parseInt(args[i + 1]);
            }
            if (arg.equals("-farm")) {
                com.inubot.api.methods.Client.GAME_TICK_SLEEP = 100;
                com.inubot.api.methods.Client.LANDSCAPE_RENDERING_ENABLED = false;
                com.inubot.api.methods.Client.MODEL_RENDERING_ENABLED = false;
            }
        }
    }

    private static byte[] read(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while (inputStream.available() > 0) {
            read = inputStream.read(buffer, 0, buffer.length);
            if (read < 0) {
                break;
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
