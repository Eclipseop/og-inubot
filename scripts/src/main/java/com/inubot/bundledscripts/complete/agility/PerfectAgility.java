package com.inubot.bundledscripts.complete.agility;

import com.inubot.api.methods.*;
import com.inubot.api.methods.traversal.Movement;
import com.inubot.api.oldschool.*;
import com.inubot.api.oldschool.action.ActionOpcodes;
import com.inubot.api.oldschool.action.tree.InputButtonAction;
import com.inubot.api.oldschool.action.tree.SelectableSpellButtonAction;
import com.inubot.api.oldschool.action.tree.TableAction;
import com.inubot.api.oldschool.event.MessageEvent;
import com.inubot.api.util.Paintable;
import com.inubot.api.util.StopWatch;
import com.inubot.api.util.Time;
import com.inubot.api.util.filter.Filter;
import com.inubot.bundledscripts.proframework.ProScript;
import com.inubot.script.Manifest;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Manifest(name = "ProAgility", developer = "Dogerina & luckruns0ut", version = 1.0, desc = "Does any course including rooftops except the barbarian course")
public class PerfectAgility extends ProScript implements Paintable {

    private static final Filter<InterfaceComponent> DIALOGUE_FILTER = w -> w.getText() != null && (w.getText().equals("Click here to continue") || w.getText().equals("Sure, I'll give it a go."));
    private static final Filter<InterfaceComponent> LOBBY_FILTER = w -> w.getText() != null && w.getText().equals("Play RuneScape");
    private static final Tile ARDY_STUCK = new Tile(2654, 3299, 3);
    private final StopWatch stucktime = new StopWatch(0);
    private Course course = null;
    private int stuck = 0;
    private boolean loggedInLast = false;

    //713 = high alch anim
    //712 = low alch anim

    private static final boolean ALCHING = true;
    private static final Filter<Item> NATURE_FILTER = (i -> i.getName().contains("rune"));
    private static final Filter<Item> OTHER_FILTER = (i -> i.getName().contains("Rune arrow"));

    @Override
    public boolean setup() {
        setLineColor(Color.RED.darker());
        setTextColor(Color.WHITE);
        //  Client.setInterfaceRendering(false);
        //JFrame frame = new JFrame();
        //frame.setLayout(new FlowLayout());
        //JComboBox<Course> courses = new JComboBox<>(Course.values());
        //frame.add(courses);
        //JButton start = new JButton("Start");
        //start.addActionListener(e -> {
//            frame.dispose();
//            course = (Course) courses.getSelectedItem();
//        });
//        frame.add(start);
//        frame.pack();
//        frame.setVisible(true);
        //while (course == null)
//            Time.sleep(400);
        return true;
    }

    @Override
    public void onFinish() {
        Client.setInterfaceRendering(true);
    }

    private void alch() {
        if (!ALCHING) {
            return;
        }
        Item runes = Inventory.getFirst(NATURE_FILTER);
        Item other = Inventory.getFirst(OTHER_FILTER);
        if (runes != null && other != null) {
            // low alch
            Client.processAction(new SelectableSpellButtonAction(14286862), "", "");
            // high alch
            //Client.processAction(new SelectableSpellButtonAction(14286883), "", "");
            Client.processAction(new TableAction(ActionOpcodes.SPELL_ON_ITEM, other.getId(), other.getIndex(), 9764864), "", "");
        }
    }

    @Override
    public int loop() {
        if (!Game.isLoggedIn()) {
            loggedInLast = false;
            return 100;
        }

        if (Players.getLocal().getAnimation() != -1 && Players.getLocal().getAnimation() != 713)
            return 100;

        if (!loggedInLast) { // if the bot has just logged in, or has just been ran
            for (Course cours : Course.values()) {
                for (Obstacle obs : cours.getObstacles()) {
                    if (obs.getLocation().contains(Players.getLocal().getLocation())) {
                        this.course = cours;
                    }
                }
            }
        }

        loggedInLast = true;

        if (this.course == null) {
            return 100;
        }

        if (!Movement.isRunEnabled() && Movement.getRunEnergy() > 10) {
            Movement.toggleRun(true);
            Time.sleep(600);
        }
        if (Skills.getCurrentLevel(Skill.HITPOINTS) < 10
                || (course == Course.ARDY_COURSE && Skills.getCurrentLevel(Skill.AGILITY) < 90)) {
            //use summer pie if ardy course
            Item food = Inventory.getFirst(item -> item.containsAction("Eat"));
            if (food != null) {
                food.processAction("Eat");
            } else {
                return 5000;
            }
        }

        if (Interfaces.getComponents(LOBBY_FILTER).length > 0) {
            for (InterfaceComponent interfaceComponent : Interfaces.getComponents(DIALOGUE_FILTER)) {
                if (interfaceComponent.getText() == null)
                    continue;
                Client.processAction(new InputButtonAction(interfaceComponent.getId()), "Play RuneScape", "");
            }
        }

        GroundItem mark = GroundItems.getNearest("Mark of grace");
        if (mark != null && Movement.isObjectReachable(mark) && mark.getLocation().getPlane() == Players.getLocal().getLocation().getPlane()) {
            mark.processAction("Take");
            return 400;
        }

        Obstacle obstacle = course.getNext();

        if (Players.getLocal().getLocation().equals(ARDY_STUCK)) {
            Movement.walkTo(new Tile(2656, 3296, 3));
            Time.sleep(1800, 2200);
            obstacle = course.getNext();
        }

        if (obstacle == null)
            return 300;


        GameObject obj;

        if (obstacle.getTile() != null) {
            if (obstacle.getTile().getPlane() == Players.getLocal().getLocation().getPlane() && obstacle.getTile().distance() > 15) {
                Movement.walkTo(obstacle.getTile());
                return 300;
            }

            final Obstacle finalObstacle = obstacle;
            obj = GameObjects.getNearest(gameObject -> {
                if (gameObject.getName() != null && gameObject.getName().equals(finalObstacle.getName())) {
                    if (gameObject.getLocation().getRegionX() == finalObstacle.getTile().getRegionX() && gameObject.getLocation().getRegionY() == finalObstacle.getTile().getRegionY())
                        return true;
                }
                return false;
            });
        } else
            obj = GameObjects.getNearest(obstacle.name);
        if (obj != null) {
            if (obj.distance() > 4 && Players.getLocal().getAnimation() != 713) { //not sure how close u have to be for alch to slow u down
                alch();
            }
            obj.processAction(obstacle.action);
            if (Time.await(() -> Players.getLocal().isMoving(), 600)) {
                Time.await(() -> Players.getLocal().getAnimation() != -1, 5000);
            }
        }
        return 300;
    }

    @Override
    public void getPaintData(Map<String, Object> data) {
        if (course != null) {
            data.put("Course", course.toString());
        }
        data.put("Experience to level", Skills.getExperienceAt(Skills.getCurrentLevel(Skill.AGILITY) + 1) - Skills.getExperience(Skill.AGILITY));
    }

    @Override
    public void messageReceived(MessageEvent e) {
        if (e.getText().contains("You can't do that from here")) {
            stuck++;
        }
    }

    public enum Course {

        GNOME_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(2472, 3438, 0), new Tile(2490, 3436, 0)),
                        "Log balance", "Walk-across", new Tile(2474, 3435))
                .append(new Area(new Tile(2470, 3429, 0), new Tile(2477, 3426, 0)),
                        "Obstacle net", "Climb-over", new Tile(2473, 3425))
                .append(new Area(new Tile(2471, 3424, 1), new Tile(2476, 3422, 1), 1),
                        "Tree branch", "Climb", new Tile(2473, 3422, 1))
                .append(new Area(new Tile(2472, 3421, 2), new Tile(2477, 3418, 2), 2),
                        "Balancing rope", "Walk-on", new Tile(2478, 3420, 2))
                .append(new Area(new Tile(2483, 3421, 2), new Tile(2488, 3418, 2), 2),
                        "Tree branch", "Climb-down", new Tile(2486, 3419, 2))
                .append(new Area(new Tile(2482, 3425, 0), new Tile(2489, 3419, 0)),
                        "Obstacle net", "Climb-over", new Tile(2487, 3426))
                .append(new Area(new Tile(2482, 3431, 0), new Tile(2490, 3427, 0)),
                        "Obstacle pipe", "Squeeze-through", new Tile(2484, 3431))
                .array()
        ),

        DRAYNOR_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(3060, 3281, 0), new Tile(3110, 3147, 0), 0),
                        "Rough wall", "Climb")
                .append(new Area(new Tile(3097, 3281, 3), new Tile(3102, 3277, 3), 3),
                        "Tightrope", "Cross")
                .append(new Area(new Tile(3088, 3276, 3), new Tile(3091, 3273, 3), 3),
                        "Tightrope", "Cross")
                .append(new Area(new Tile(3089, 3267, 3), new Tile(3094, 3265, 3), 3),
                        "Narrow wall", "Balance")
                .append(new Area(new Tile(3088, 3261, 3), new Tile(3088, 3257, 3), 3),
                        "Wall", "Jump-up")
                .append(new Area(new Tile(3088, 3255, 3), new Tile(3094, 3255, 3), 3),
                        "Gap", "Jump")
                .append(new Area(new Tile(3096, 3621, 3), new Tile(3101, 3256, 3), 3),
                        "Crate", "Climb-down")
                .array()
        ),

        VARROCK_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(3249, 3392, 0), new Tile(3186, 3431, 0), 0),
                        "Rough wall", "Climb")
                .append(new Area(new Tile(3219, 3419, 3), new Tile(3214, 3410, 3), 3),
                        "Clothes line", "Cross")
                .append(new Area(new Tile(3208, 3414, 3), new Tile(3201, 3417, 3), 3),
                        "Gap", "Leap")
                .append(new Area(new Tile(3197, 3416, 1), new Tile(3193, 3416, 1), 1),
                        "Wall", "Balance")
                .append(new Area(new Tile(3192, 3406, 3), new Tile(3198, 3402, 3), 3),
                        "Gap", "Leap", new Tile(3193, 3401, 3))
                .append(new Area(new Tile(3182, 3382, 3), new Tile(3208, 3398, 3), 3),
                        "Gap", "Leap", new Tile(3209, 3397, 3))
                .append(new Area(new Tile(3218, 3393, 3), new Tile(3232, 3402, 3), 3),
                        "Gap", "Leap", new Tile(3233, 3402, 3))
                .append(new Area(new Tile(3236, 3403, 3), new Tile(3240, 3408, 3), 3),
                        "Ledge", "Hurdle")
                .append(new Area(new Tile(3240, 3410, 3), new Tile(3236, 3415, 3), 3),
                        "Edge", "Jump-off")
                .array()
        ),

        CANFIS_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(3465, 3470, 0), new Tile(3522, 3518, 0), 0),
                        "Tall tree", "Climb")
                .append(new Area(new Tile(3502, 3491, 2), new Tile(3511, 3498, 2), 2),
                        "Gap", "Jump", new Tile(3505, 3498, 2))
                .append(new Area(new Tile(3496, 3503, 2), new Tile(3504, 3507, 2), 2),
                        "Gap", "Jump", new Tile(3496, 3504, 2))
                .append(new Area(new Tile(3485, 3498, 2), new Tile(3493, 3505, 2), 2),
                        "Gap", "Jump", new Tile(3485, 3499, 2))//
                .append(new Area(new Tile(3474, 3491, 3), new Tile(3480, 3500, 3), 3),
                        "Gap", "Jump", new Tile(3478, 3491, 3))//
                .append(new Area(new Tile(3476, 3480, 2), new Tile(3484, 3487, 2), 2),
                        "Pole-vault", "Vault")
                .append(new Area(new Tile(3486, 3468, 3), new Tile(3504, 3479, 3), 3),
                        "Gap", "Jump", new Tile(3503, 3476, 3))
                .append(new Area(new Tile(3509, 3474, 2), new Tile(3516, 3483, 2), 2),
                        "Gap", "Jump",  new Tile(3510, 3483, 2))
                .array()
        ),

        FALADOR_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(3003, 3363, 0), new Tile(3059, 3328, 0), 0),
                        "Rough wall", "Climb")
                .append(new Area(new Tile(3036, 3343, 3), new Tile(3040, 3342, 3), 3),
                        "Tightrope", "Cross", new Tile(3040, 3343, 3))
                .append(new Area(new Tile(3045, 3349, 3), new Tile(3051, 3341, 3), 3),
                        "Hand holds", "Cross", new Tile(3050, 3350, 3))
                .append(new Area(new Tile(3048, 3358, 3), new Tile(3050, 3357, 3), 3),
                        "Gap", "Jump", new Tile(3048, 3359, 3))
                .append(new Area(new Tile(3045, 3367, 3), new Tile(3048, 3361, 3), 3),
                        "Gap", "Jump", new Tile(3044, 3361, 3))
                .append(new Area(new Tile(3034, 3364, 3), new Tile(3041, 3361, 3), 3),
                        "Tightrope", "Cross", new Tile(3034, 3361, 3))
                .append(new Area(new Tile(3026, 3354, 3), new Tile(3029, 3352, 3), 3),
                        "Tightrope", "Cross", new Tile(3026, 3353, 3))
                .append(new Area(new Tile(3009, 3358, 3), new Tile(3021, 3353, 3), 3),
                        "Gap", "Jump", new Tile(3016, 3352, 3))
                .append(new Area(new Tile(3016, 3349, 3), new Tile(3022, 3343, 3), 3),
                        "Ledge", "Jump", new Tile(3015, 3345, 3))
                .append(new Area(new Tile(3011, 3346, 3), new Tile(3014, 3344, 3), 3),
                        "Ledge", "Jump", new Tile(3011, 3343, 3))// the first bendy corner bit
                .append(new Area(new Tile(3009, 3342, 3), new Tile(3013, 3335, 3), 3),
                        "Ledge", "Jump", new Tile(3012, 3334, 3))
                .append(new Area(new Tile(3012, 3334, 3), new Tile(3017, 3331, 3), 3),
                        "Ledge", "Jump", new Tile(3018, 3332, 3))
                .append(new Area(new Tile(3019, 3335, 3), new Tile(3024, 3332, 3), 3),
                        "Edge", "Jump", new Tile(3025, 3332, 3))
                .array()
        ),

        SEERS_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(2682, 3511, 0), new Tile(2728, 3451, 0), 0),
                        "Wall", "Climb-up")
                .append(new Area(new Tile(2721, 3497, 3), new Tile(2730, 3490, 3), 3),
                        "Gap", "Jump", new Tile(2720, 3492, 3))
                .append(new Area(new Tile(2705, 3495, 2), new Tile(2713, 3488, 2), 2),
                        "Tightrope", "Cross", new Tile(2710, 3489, 2))
                .append(new Area(new Tile(2710, 3481, 2), new Tile(2715, 3477, 2), 2),
                        "Gap", "Jump", new Tile(2710, 3476, 2))
                .append(new Area(new Tile(2700, 3475, 3), new Tile(2715, 3470, 3), 3),
                        "Gap", "Jump", new Tile(2700, 3469, 3))
                .append(new Area(new Tile(2698, 3475, 2), new Tile(2702, 3460, 2), 2),
                        "Edge", "Jump", new Tile(2703, 3461, 2))
                .array()
        ),

        POLLNIVNEACH_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(3344, 3003, 0), new Tile(3400, 2900, 0), 0), "Basket", "Climb-on", new Tile(3351, 2962))
                .append(new Area(new Tile(3351, 2961, 1), new Tile(3346, 2968, 1), 1), "Market stall", "Jump-on")
                .append(new Area(new Tile(3352, 2973, 1), new Tile(3355, 2976, 1), 1), "Banner", "Grab")
                .append(new Area(new Tile(3360, 2977, 1), new Tile(3362, 2979, 1), 1), "Gap", "Leap")
                .append(new Area(new Tile(3366, 2976, 1), new Tile(3369, 2974, 1), 1), "Tree", "Jump-to")
                .append(new Area(new Tile(3369, 2982, 1), new Tile(3365, 2986, 1), 1), "Rough wall", "Climb")
                .append(new Area(new Tile(3365, 2985, 2), new Tile(3355, 2981, 2), 2), "Monkeybars", "Cross")
                .append(new Area(new Tile(3357, 2995, 2), new Tile(3370, 2990, 2), 2), "Tree", "Jump-on")
                .append(new Area(new Tile(3356, 3000, 2), new Tile(3362, 3004, 2), 2), "Drying line", "Jump-to")
                .array()
        ),

        RELLEKKA_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(2675, 3647, 0), new Tile(2620, 3681, 0), 0), "Rough wall", "Climb")
                .append(new Area(new Tile(2626, 3676, 3), new Tile(2622, 3672, 3), 3), "Gap", "Leap")
                .append(new Area(new Tile(2622, 3668, 3), new Tile(2615, 3658, 3), 3), "Tightrope", "Cross")
                .append(new Area(new Tile(2626, 3651, 3), new Tile(2629, 3655, 3), 3), "Gap", "Leap")
                .append(new Area(new Tile(2639, 3653, 3), new Tile(2643, 3649, 3), 3), "Gap", "Hurdle")
                .append(new Area(new Tile(2643, 3657, 3), new Tile(2650, 3662, 3), 3), "Tightrope", "Cross")
                .append(new Area(new Tile(2655, 3665, 3), new Tile(2663, 3685, 3), 3), "Pile of fish", "Jump-in")
                .array()
        ),

        ARDY_COURSE(ObstacleFactory.newInstance(true)
                .append(new Area(new Tile(2647, 3327, 0), new Tile(2679, 3286, 0), 0), "Wooden Beams", "Climb-up")
                .append(new Area(new Tile(2670, 3310, 3), new Tile(2672, 3297, 3), 3), "Gap", "Jump")
                .append(new Area(new Tile(2660, 3320, 3), new Tile(2666, 3316, 3), 3), "Plank", "Walk-on")
                .append(new Area(new Tile(2652, 3320, 3), new Tile(2657, 3316, 3), 3), "Gap", "Jump")
                .append(new Area(new Tile(2652, 3315, 3), new Tile(2654, 3309, 3), 3), "Gap", "Jump", new Tile(2653, 3308, 3))
                .append(new Area(new Tile(2650, 3310, 3), new Tile(2655, 3299, 3), 3), "Steep roof", "Balance-across", new Tile(2654, 3300, 3))
                .append(new Area(new Tile(2654, 3300, 3), new Tile(2658, 3296, 3), 3), "Gap", "Jump", new Tile(2656, 3296, 3))
                .array()
        );

        private final Obstacle[] obstacles;

        private Course(Obstacle[] obstacles) {
            this.obstacles = obstacles;
        }

        private Obstacle getNext() {
            for (Obstacle obstacle : obstacles) {
                if (obstacle.getLocation().contains(Players.getLocal()))
                    return obstacle;
            }
            return null;
        }

        @Override
        public String toString() {
            String name = super.name();
            return name.charAt(0) + name.substring(1).toLowerCase().replace('_', ' ');
        }

        public Obstacle[] getObstacles() {
            return obstacles;
        }
    }

    private static class Obstacle {

        private final Area location;
        private final String name;
        private final String action;
        private final Tile tile; // exact location for obstacle, only needed if script tries 2 do prev obstacle agen
        private final boolean npc;

        public Obstacle(Area location, String name, String action, boolean npc, Tile tile) {
            this.location = location;
            this.name = name;
            this.action = action;
            this.npc = npc;
            this.tile = tile;
        }

        public Obstacle(Area location, String name, String action, Tile tile) {
            this(location, name, action, false, tile);
        }

        public Obstacle(Area location, String name, String action) {
            this(location, name, action, false, null);
        }

        public Area getLocation() {
            return location;
        }

        public String getAction() {
            return action;
        }

        public String getName() {
            return name;
        }

        public Tile getTile() {
            return tile;
        }

        public boolean isNpc() {
            return npc;
        }
    }

    private static class ObstacleFactory {

        private static final List<Obstacle> obstacles = new ArrayList<>();

        private ObstacleFactory() {

        }

        public static ObstacleFactory newInstance(boolean clear) {
            if (clear)
                obstacles.clear();
            return new ObstacleFactory();
        }

        public static ObstacleFactory newInstance() {
            return newInstance(false);
        }

        public ObstacleFactory append(Area loc, String action, String target, boolean npc, Tile tile) {
            obstacles.add(new Obstacle(loc, action, target, npc, tile));
            return this;
        }

        public ObstacleFactory append(Area loc, String action, String target, Tile tile) {
            return append(loc, action, target, false, tile);
        }

        public ObstacleFactory append(Area loc, String action, String target) {
            return append(loc, action, target, false, null);
        }

        public Obstacle[] array() {
            return obstacles.toArray(new Obstacle[obstacles.size()]);
        }
    }
}

