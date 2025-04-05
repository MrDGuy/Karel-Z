package test;

import java.awt.Color;
import java.io.File;
import karelz.*;

public class RobotTester {
    public static void main(String[] args) {
        // Load the world from file
        World world = new World();
        world.loadWorld(new File("src/worlds/worldguy1.kzw")); // adjust path if needed

        // Create a robot and queue instructions
        Robot robot = new Robot(0, 0, Direction.RIGHT, 1, Color.BLUE, world).withLogging();
        // Queue up instructions BEFORE launchThread()
        robot.move();
        robot.move();
        robot.move();
        robot.turnLeft();
        robot.move();
        robot.move();
        robot.pickBeeper();
        robot.putBeeper();
        robot.putBeeper();
        robot.turnRight();
        robot.turnRight();
        robot.move();
        robot.move();
        robot.turnOff();
        world.add(robot);

        // Create the window for animation
        Window window = new Window(world, 500, false, true); 
        window.setVisible(true);
        robot.setStepDelay(window.getDelay());
       
        System.out.println("Logging enabled: " + robot.getLogging());
    }
}