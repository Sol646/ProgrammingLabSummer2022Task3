package com.dasher.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;


// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DasherSlasher");
        config.setWindowIcon("Goblin.png");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);
        config.setResizable(false);
        new Lwjgl3Application(new DasherMain(), config);
    }
}
