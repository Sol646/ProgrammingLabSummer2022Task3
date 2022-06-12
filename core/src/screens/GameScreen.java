package screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.dasher.game.DasherMain;
import com.dasher.game.Enemy;
import com.dasher.game.Player;
import com.dasher.game.managers.CollListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.dasher.game.DasherMain.PPM;

public class GameScreen extends AbstractScreen {
    public static CHARACTER_CLASS type;

    public enum COLLISIONS {
        PLAYER((byte) 1), KNIGHT((byte) 2), WARRIOR((byte) 4), DEATHZONE((byte) 8);
        public byte mask;

        COLLISIONS(byte s) {
            this.mask = s;
        }
    }

    public enum CHARACTER_CLASS {
        GOBLIN, HOBGOBLIN
    }

    OrthographicCamera camera;
    public World world;
    //Box2DDebugRenderer b2rd;
    private Body deathZoneLeft, deathZoneRight, deathZoneTop, deathZoneBottom, box;
    public static Player player;
    public static List<Enemy> enemyList;
    private Texture pTex;
    private Texture kTex;
    private Texture wTex;
    private Texture earth;
    private Sound dashSound;
    private long lastDashTime;
    private long lastEnemySpawn;
    private byte enemyCounter;
    float i = 0;


    private final Vector3 touchPos = new Vector3();
    private final Vector2 target = new Vector2();
    private final Vector2 t = new Vector2();

    public GameScreen(DasherMain app) {
        super(app);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 1280, 720);
        camera.position.set(0, 0, 0);
        camera.update();

        enemyList = new ArrayList<Enemy>();
        world = new World(new Vector2(0f, 0f), false);
        //b2rd = new Box2DDebugRenderer();
        earth = new Texture("earthBack.png");
        dashSound = Gdx.audio.newSound(Gdx.files.internal("dash.mp3"));
        lastDashTime = 0;
        enemyCounter = 0;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.clear();
        world.setContactListener(new CollListener());
        app.batch.setProjectionMatrix(camera.combined);
        pTex = type.equals(CHARACTER_CLASS.GOBLIN) ? new Texture("Goblin.png") :
                new Texture("Hobgoblin.png");
        kTex = new Texture("Knight.png");
        wTex = new Texture("Warrior.png");
        player = new Player(type, createBox
                (0.4f, 0.5f, 26, 32, COLLISIONS.PLAYER, BodyDef.BodyType.DynamicBody, 10f, false));
        deathZoneTop = createBox
                (0.295f, 4.8f, 547, 16, COLLISIONS.DEATHZONE, BodyDef.BodyType.StaticBody, 0f, true);
        deathZoneBottom = createBox
                (0.295f, -5.4f, 547, 16, COLLISIONS.DEATHZONE, BodyDef.BodyType.StaticBody, 0f, true);
        deathZoneLeft = createBox
                (-8f, -0.43f, 16, 350, COLLISIONS.DEATHZONE, BodyDef.BodyType.StaticBody, 0f, true);
        deathZoneRight = createBox
                (8.6f, -0.43f, 16, 350, COLLISIONS.DEATHZONE, BodyDef.BodyType.StaticBody, 0f, true);
    }

    @Override
    public void update(float delta) {
        stage.act(delta);
        world.step(1 / 120f, 12, 4);
        if (player.isAlive) inputUpdate();
        for (Iterator<Enemy> i = enemyList.iterator(); i.hasNext(); ) {
            Enemy enemy = i.next();
            t.set(player.body.getPosition().sub(enemy.body.getPosition()));
            t.nor();
            enemy.body.setLinearVelocity(t);
            if(enemy.hp <= 0) {
                i.remove();
                world.destroyBody(enemy.body);
                enemyCounter--;
            }
        }
        if(TimeUtils.nanoTime() - lastEnemySpawn > 1000000000 && enemyCounter <= 15) {
            enemySpawner();
            enemyCounter++;
        }
        if (player.hp <= 0) player.isAlive = false;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        ScreenUtils.clear(Color.valueOf("709f6e"));
        stage.draw();
        app.batch.begin();
        app.batch.draw(earth, -earth.getWidth() / 2, -earth.getHeight() / 2);
        app.batch.draw(pTex, player.body.getPosition().x * PPM - (pTex.getWidth() / 2),
                player.body.getPosition().y * PPM - (pTex.getHeight() / 2));
        for (Enemy enemy : enemyList) {
            switch (enemy.getType()) {
                case "KNIGHT":
                    app.batch.draw(kTex, enemy.body.getPosition().x * PPM - 26,
                            enemy.body.getPosition().y * PPM - 32);
                    break;
                case "WARRIOR":
                    app.batch.draw(wTex, enemy.body.getPosition().x * PPM - 26,
                            enemy.body.getPosition().y * PPM - 32);
                    break;
            }
        }
        app.batch.end();
        //b2rd.render(world, camera.combined.cpy().scl(PPM));
    }

    @Override
    public void dispose() {
        super.dispose();
        earth.dispose();
        pTex.dispose();
        kTex.dispose();
        wTex.dispose();
        dashSound.dispose();
        world.dispose();
        //b2rd.dispose();
    }

    private void inputUpdate() {
        long DASH_DELAY = 800000000; // откат рывка 0.8
        player.isDash = !(TimeUtils.nanoTime() - lastDashTime >= 250000000);
        if (TimeUtils.nanoTime() - lastDashTime > DASH_DELAY) {
            if (Gdx.input.isTouched()) {
                lastDashTime = TimeUtils.nanoTime();
                player.isDash = true;
                dashSound.play(0.4f);
                touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPos);
                target.set(touchPos.x / PPM - player.body.getPosition().x, touchPos.y / PPM - player.body.getPosition().y);
                target.nor();
                target.set(target.x * player.getMoveSpeed(), target.y * player.getMoveSpeed());
                player.body.setLinearVelocity(target);
                lastDashTime = TimeUtils.nanoTime();
            }
        }
    }

    private Body createBox(float x, float y, float width, float height,
                           COLLISIONS ctg, BodyDef.BodyType type, float damping, boolean isSensor) {
        Body body;
        BodyDef definitions = new BodyDef();
        definitions.type = type;
        definitions.position.set(x, y);
        definitions.fixedRotation = true;
        definitions.linearDamping = type == BodyDef.BodyType.DynamicBody ? damping : 0f;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / PPM, height / PPM);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1.0f;
        fixtureDef.shape = shape;
        fixtureDef.isSensor = isSensor;

        body = world.createBody(definitions);
        body.createFixture(fixtureDef);
        body.setUserData(ctg.toString());
        return body;
    }

    private void enemySpawner() {
        int type = MathUtils.random(1, 2);
        Enemy enemy = new Enemy(type == 2 ? Enemy.ENEMY_TYPE.WARRIOR : Enemy.ENEMY_TYPE.KNIGHT,
                createBox(MathUtils.random(-7f, 8f), MathUtils.random(-4.5f, 3.5f),
                        26, 32, type == 2 ? COLLISIONS.WARRIOR : COLLISIONS.KNIGHT,
                        BodyDef.BodyType.DynamicBody, 10f, true));
        enemyList.add(enemy);
        lastEnemySpawn = TimeUtils.nanoTime();
    }
}
