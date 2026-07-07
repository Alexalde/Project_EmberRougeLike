package com.github.alexalde.emberroguelike;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player; // Hier ist unser Spieler-Objekt!

    @Override
    public void create() {
        batch = new SpriteBatch();
        // Wir instanziieren unseren Spieler bei X:200, Y:200
        player = new Player(200, 200);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // 1. Logik updaten
        player.update(deltaTime);

        // 2. Bildschirm leeren
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // 3. Zeichnen
        batch.begin();
        player.draw(batch); // Wir übergeben dem Spieler die "Mal-Hand"
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose(); // Auch der Spieler muss seinen Speicher aufräumen
    }
}
