package karelz;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Robot {
    int x;
    int y;
    Direction direction;
    int beepers;
    RobotImageCollection collection;
    RobotState state;
    World world;
    boolean logging;
    long stepCount;

    Thread thread;
    volatile boolean threadIsActive;
    private int stepDelay = 300; // Delay between steps
    private Runnable repaintCallback;
    private final List<Runnable> instructionQueue = new ArrayList<>();
    private boolean recording = true;
    private int initialX;
    private int initialY;
    private Direction initialDirection;
    private int initialBeepers;
    private List<Runnable> originalInstructions = new ArrayList<>();

    public void setRepaintCallback(Runnable repaintCallback) {
        this.repaintCallback = repaintCallback;
    }

    public Robot(int x, int y, Direction direction) {
        this(x, y, direction, 0, null, null);
    }

    public Robot(int x, int y, Direction direction, int beepers) {
        this(x, y, direction, beepers, null, null);
    }

    public Robot(int x, int y, Direction direction, Color color) {
        this(x, y, direction, 0, color, null);
    }

    public Robot(int x, int y, Direction direction, int beepers, Color color) {
        this(x, y, direction, beepers, color, null);
    }

    public Robot(int x, int y, Direction direction, int beepers, Color color, World world) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.beepers = beepers;
        collection = new RobotImageCollection(color);
        state = RobotState.ON;
        this.world = world;
        logging = false;
        stepCount = 0;
        this.initialX = x;
        this.initialY = y;
        this.initialDirection = direction;
        this.initialBeepers = beepers;
    }

    public void task() throws EndTaskException {
        recording = false;
        // System.out.println("🔧 Robot thread started");
        // log("Starting task with " + instructionQueue.size() + " instructions");int i = 1;
        // for (Runnable r : instructionQueue) {
        //     //log("Executing step " + i);
        //     r.run();
        //     i++;
        // }
    }

    public void launchThread() {
        this.state = RobotState.ON;
        threadIsActive = true;
        thread = new Thread(() -> {
            System.out.println("🚀 Robot thread started"); // Confirm thread launches
            try {
                task(); // executes the queued instructions
            } catch (Exception e) {
                System.out.println("❌ Exception in task: " + e);
                e.printStackTrace();
            }
            // threadIsActive = false;
            // if (state != RobotState.ERROR) {
            //     log("finished its task in " + stepCount + (stepCount == 1 ? " step" : " steps"));
            // }
        });
        thread.start();
    }
    

    public void step() {
        if (running()) {
            stepCount++;
            log("stepped, facing " + direction.toString().toLowerCase() + " with " + (beepers == Cell.INFINITY ? "infinity" : beepers) + (beepers == 1 ? " beeper" : " beepers"));

            if (repaintCallback != null) {
                repaintCallback.run();
            }

            try {
                Thread.sleep(stepDelay); // Delay between steps
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void executeNext() {
        if (!instructionQueue.isEmpty() && running()) {
            Runnable next = instructionQueue.remove(0);
            next.run();
        }   else if (instructionQueue.isEmpty()) {
            threadIsActive = false;
            log("✅ Finished all steps.");
        }
    }

    public void waitForStep() {
        while (threadIsActive) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void log(Object message) {
        if (logging) {
            System.out.println("A " + getClass().getSimpleName() + " at (" + x + ", " + y + ") has " + message);
        }
    }

    public void resetToInitialState() {
        this.x = initialX;
        this.y = initialY;
        this.direction = initialDirection;
        this.beepers = initialBeepers;
        this.state = RobotState.ON;
        this.threadIsActive = false;
        this.stepCount = 0;
        this.instructionQueue.clear();
        this.instructionQueue.addAll(originalInstructions);
    }

    public Robot withLogging() {
        logging = true;
        return this;
    }

    public void setLogging(boolean value) {
        logging = value;
    }

    public boolean getLogging() {
        return logging;
    }

    public long getStepCount() {
        return stepCount;
    }

    public boolean frontIsClear() {
        Cell currentCell = world.get(x, y);
        if ((currentCell.containsHorizontalWall() && direction == Direction.DOWN) ||
            (currentCell.containsVerticalWall() && direction == Direction.LEFT)) {
            return false;
        }
        int nextX = x;
        int nextY = y;
        switch (direction) {
            case UP: nextY++; break;
            case RIGHT: nextX++; break;
            case DOWN: nextY--; break;
            case LEFT: nextX--; break;
        }
        if (nextX < 0 || nextY < 0) return false;
        Cell nextCell = world.get(nextX, nextY);
        if ((nextCell.containsHorizontalWall() && direction == Direction.UP) ||
            (nextCell.containsVerticalWall() && direction == Direction.RIGHT) ||
            nextCell.containsBlockWall()) {
            return false;
        }
        return true;
    }

    public boolean hasBeepers() {
        return beepers > 0 || beepers == Cell.INFINITY;
    }

    public boolean nextToABeeper() {
        return world.get(x, y).containsValidBeeperPile();
    }

    public boolean nextToARobot() {
        return world.robots.stream().filter(robot -> x == robot.x && y == robot.y).count() >= 2;
    }

    public boolean facingUp() { return direction == Direction.UP; }
    public boolean facingRight() { return direction == Direction.RIGHT; }
    public boolean facingDown() { return direction == Direction.DOWN; }
    public boolean facingLeft() { return direction == Direction.LEFT; }
    public boolean running() { return state == RobotState.ON; }

    public boolean frontIsBlocked() { return !frontIsClear(); }
    public boolean doesntHaveBeepers() { return !hasBeepers(); }
    public boolean notNextToABeeper() { return !nextToABeeper(); }
    public boolean notNextToARobot() { return !nextToARobot(); }
    public boolean notFacingUp() { return !facingUp(); }
    public boolean notFacingRight() { return !facingRight(); }
    public boolean notFacingDown() { return !facingDown(); }
    public boolean notFacingLeft() { return !facingLeft(); }
    public boolean notRunning() { return !running(); }

    public void iterate(int times, CodeBlock code) {
        for (int i = 0; i < times && running(); i++) {
            try {
                code.execute();
            } catch (Exception e) {}
        }
    }

    public void turnOff() {
        Runnable action = () -> {
            log("turning off");
            if (running()) {
                state = RobotState.OFF;
                threadIsActive = false;
                log("turned off");
            }
            step();
        };
        if (recording) instructionQueue.add(action); else action.run();
    }

    public void sleep() {
        if (running()) {
            waitForStep();
        }
    }

    public void move() {
        Runnable action = () -> {
            log("moving forward");
            if (!running()) return;
            try {
                if (frontIsClear()) {
                    switch (direction) {
                        case UP: y++; break;
                        case RIGHT: x++; break;
                        case DOWN: y--; break;
                        case LEFT: x--; break;
                    }
                } else {
                    throw new EndTaskException();
                }
            } catch (EndTaskException e) {
                state = RobotState.ERROR;
                threadIsActive = false;
                log("crashed: Hit a wall");
            }
            step();
        };
        if (recording) {
            instructionQueue.add(action);
            originalInstructions.add(action); // 👈 store for reset
        } else {
            action.run();
        }
    }

    public void turnLeft() {
        Runnable action = () -> {
            log("turning left");
            if (running()) {
                direction = direction.getCounterclockwiseDirection();
            }
            step();
        };
        if (recording) {
            instructionQueue.add(action);
            originalInstructions.add(action); // 👈 store for reset
        } else {
            action.run();
        }
    }

    public void turnRight() {
        Runnable action = () -> {
            log("turning right");
            if (running()) {
                direction = direction.getClockwiseDirection();
            }
            step();
        };
        if (recording) {
            instructionQueue.add(action);
            originalInstructions.add(action); // 👈 store for reset
        } else {
            action.run();
        }
    }

    public void putBeeper() {
        Runnable action = () -> {
            log("putting beeper");
            if (!running()) return;
            try {
                if (hasBeepers() && beepers != Cell.INFINITY) {
                    beepers--;
                    world.add(x, y, Cell.newBeeperPile());
                } else {
                    throw new EndTaskException();
                }
            } catch (EndTaskException e) {
                state = RobotState.ERROR;
                threadIsActive = false;
                log("crashed: Tried to put beeper when it didn't have one");
            }
            step();
        };
        if (recording) {
            instructionQueue.add(action);
            originalInstructions.add(action); // 👈 store for reset
        } else {
            action.run();
        }
    }

    public void pickBeeper() {
        Runnable action = () -> {
            log("picking beeper");
            if (!running()) return;
            try {
                Cell currentCell = world.get(x, y);
                if (currentCell.containsValidBeeperPile()) {
                    if (currentCell.beepers != Cell.INFINITY) {
                        currentCell.beepers--;
                    }
                    currentCell.clearBeeperPileIfEmpty();
                    if (beepers != Cell.INFINITY) {
                        beepers++;
                    }
                } else {
                    throw new EndTaskException();
                }
            } catch (EndTaskException e) {
                state = RobotState.ERROR;
                threadIsActive = false;
                log("crashed: Tried to pick beeper when there was none");
            }
            step();
        };
        if (recording) {
            instructionQueue.add(action);
            originalInstructions.add(action); // 👈 store for reset
        } else {
            action.run();
        }
    }

    public BufferedImage getCurrentImage() {
        return collection.getImage(direction, state);
    }

    public void setStepDelay(int milliseconds) {
        // update delay used in step()
        stepDelay = milliseconds;
    }
} 
