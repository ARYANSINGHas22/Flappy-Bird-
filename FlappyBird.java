import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.image.ImageObserver;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

// Bird class is used to create a bird object in the game and to move it around the screen
class Bird extends GameObject {
    private ProxyImage proxyImage; // ProxyImage object used to load the image of the bird
    private Tube[] tube; // Array of Tube objects used to create the walls in the game
    private Clip chirpSound; // Sound for bird chirping
    private double difficultyFactor; // Factor to increase difficulty over time
    private String birdImage; // Added variable to store bird image filename

    // Modified constructor to accept bird image filename
    public Bird(int x, int y, String birdImage){
        super(x, y);
        this.birdImage = birdImage; // Store the bird image filename
        if(proxyImage == null) {
            proxyImage = new ProxyImage(birdImage); // Use the provided image
        }
        this.image = proxyImage.loadImage().getImage();
        this.width = image.getWidth(null); //Set the width and height of the bird
        this.height = image.getHeight(null);
        this.x -= width; // Adjust the x position of the bird
        this.y -= height; // Adjust the y position of the bird
        tube = new Tube[1]; // Create a new array of Tube objects
        tube[0] = new Tube(Window.WIDTH, Window.HEIGHT - 60); // Create the first wall
        this.dy = 1; // Start with slow falling speed
        this.difficultyFactor = 1.0; // Start with normal difficulty

        // Load chirp sound - direct file access
        try {
            File soundFile = new File("chirp.wav");
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            chirpSound = AudioSystem.getClip();
            chirpSound.open(audioInputStream);
        } catch (Exception e) {
            System.out.println("Error loading chirp sound: " + e.getMessage());
            createSilentClip(chirpSound);
        }
    }

    // Method to create a silent clip if sound loading fails
    private void createSilentClip(Clip clip) {
        try {
            // Create a 0.1-second silent audio format
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            byte[] silentData = new byte[4410]; // 0.1 second of silence
            AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(silentData),
                    format, silentData.length);

            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {
            System.out.println("Failed to create silent clip: " + e.getMessage());
        }
    }

    // Set the difficulty factor
    public void setDifficultyFactor(double factor) {
        this.difficultyFactor = factor;
    }

    // Method used to move the bird
    public void tick() {
        // Calculate gravity effect based on difficulty factor
        double gravityEffect = 1.0 * difficultyFactor;
        if(gravityEffect > 1.8) gravityEffect = 1.8; // Cap gravity effect

        if(dy < 4 * difficultyFactor) { // Max fall speed increases with difficulty
            dy += gravityEffect; // Gravity acceleration increases with difficulty
        }
        this.y += dy; // Move the bird down the screen
        tube[0].tick(); // Move the wall down the screen
        checkWindowBorder(); // Check if the bird has hit the top or bottom of the screen
    }

    public void jump() {
        if(dy > 0) { // If the speed of the bird is greater than 0
            dy = 0; // Set the speed of the bird to 0
        }
        // Jump power adjusts slightly with difficulty - harder modes need stronger jumps
        double jumpPower = 12.0 * (0.9 + difficultyFactor * 0.1);

        // Keeping jump power in reasonable range
        if(jumpPower < 11) jumpPower = 11;
        if(jumpPower > 14) jumpPower = 14;

        dy -= jumpPower;

        // Play chirp sound when bird jumps
        playChirpSound();
    }

    // Method to play the chirping sound
    private void playChirpSound() {
        if (chirpSound != null) {
            if (chirpSound.isRunning()) {
                chirpSound.stop();
            }
            chirpSound.setFramePosition(0);
            chirpSound.start();
        }
    }

    // Method used to check if the bird has hit the top or bottom of the screen
    private void checkWindowBorder() {
        if(this.x > Window.WIDTH) { // If the bird has moved off the right side of the screen
            this.x = Window.WIDTH; // Set the x position of the bird to the right side of the screen
        }
        if(this.x < 0) { // If the bird has moved off the left side of the screen
            this.x = 0; // Set the x position of the bird to the left side of the screen
        }
        if(this.y > Window.HEIGHT - 50) { // If the bird has moved off the bottom of the screen
            this.y = Window.HEIGHT - 50; // Set the y position of the bird to the bottom of the screen
        }
        if(this.y < 0) { // If the bird has moved off the top of the screen
            this.y = 0; // Set the y position of the bird to the top of the screen
        }
    }

    // Method used to check if the bird has hit the wall
    public void render(Graphics2D g, ImageObserver obs) {
        g.drawImage(image, x, y, obs); // Draw the bird
        tube[0].render(g, obs); // Draw the wall
    }


    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

// Tube class is used to create a wall object in the game and to move it around the screen
class TubeColumn {
    private int base = Window.HEIGHT - 60;
    private int gapSize;  // Size of the gap between tubes
    private int gapPosition;  // Position of the gap (y-coordinate)

    private List<Tube> tubes;
    private Random random;
    private int points = 0; // Variable used to keep track of the score
    private double speed = 3.0; // Initial tube speed
    private double difficultyFactor = 1.0; // Factor to scale difficulty

    public TubeColumn() {
        tubes = new ArrayList<>();
        random = new Random();
        gapSize = Window.HEIGHT / 3;  // Gap is 1/3 of screen height
        initTubes();
    }

    // Set the difficulty factor
    public void setDifficultyFactor(double factor) {
        this.difficultyFactor = factor;

        // Update speed for all existing tubes
        for (Tube tube : tubes) {
            tube.setDx((int)(speed * difficultyFactor));
        }
    }

    // Method used to create the wall
    private void initTubes() {
        tubes.clear();  // Clear existing tubes

        // Calculate a random position for the gap
        gapPosition = random.nextInt(Window.HEIGHT - gapSize - 100) + 50;

        // Calculate the gap size - decreases slightly as difficulty increases
        int currentGapSize = (int)(gapSize * (1.1 - difficultyFactor * 0.05));
        if (currentGapSize < Window.HEIGHT / 4) {
            currentGapSize = Window.HEIGHT / 4; // Minimum gap size is 1/4 of screen height
        }

        // Create top tube (tube above the gap)
        Tube topTube = new Tube(Window.WIDTH, 0);
        // Make tubes thinner at start, gradually increase width with difficulty
        int tubeWidth = (int)(Window.WIDTH / (12 - difficultyFactor * 0.5));
        if (tubeWidth > Window.WIDTH / 8) tubeWidth = Window.WIDTH / 8; // Maximum width cap

        topTube.scaleWidth(tubeWidth);
        topTube.setHeight(gapPosition);  // The height is the gap position
        topTube.setDx((int)(speed * difficultyFactor));
        tubes.add(topTube);

        // Create bottom tube (tube below the gap)
        Tube bottomTube = new Tube(Window.WIDTH, gapPosition + currentGapSize);
        bottomTube.scaleWidth(tubeWidth);
        bottomTube.setHeight(Window.HEIGHT - gapPosition - currentGapSize);
        bottomTube.setDx((int)(speed * difficultyFactor));
        tubes.add(bottomTube);
    }

    // Method used to check the position of the walls and to create new walls
    public void tick() {
        for (int i = 0; i < tubes.size(); i++) { // Loop through the array of Tube objects
            tubes.get(i).tick(); // Update the position of the wall
        }

        // Check if tubes have moved off screen
        if (!tubes.isEmpty() && tubes.get(0).getX() + tubes.get(0).getWidth() < 0) {
            tubes.clear();  // Remove offscreen tubes
            this.points += 1; // Increase the score by 1

            // Gradually increase difficulty based on points
            speed = 3.0 + (points / 10.0);
            if (speed > 6.5) speed = 6.5; // Cap max speed

            initTubes(); // Create new tubes
        }
    }

    // Method used to draw the walls
    public void render(Graphics2D g, ImageObserver obs) {
        for (int i = 0; i < tubes.size(); i++) { // Loop through the array of Tube objects
            tubes.get(i).render(g, obs); // Draw the wall
        }
    }

    public List<Tube> getTubes() {
        return tubes;
    }

    public void setTubes(List<Tube> tubes) {
        this.tubes = tubes;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public double getSpeed() {
        return speed;
    }
}

interface IStrategy {
    public void controller(Bird bird, KeyEvent kevent);
    public void controllerReleased(Bird bird, KeyEvent kevent);
}

// Controller class is used to control the movement of the bird
class Controller implements IStrategy {
    public void controller(Bird bird, KeyEvent kevent) {
    }

    public void controllerReleased(Bird bird, KeyEvent kevent) {
        if(kevent.getKeyCode() == KeyEvent.VK_SPACE) { // If the space bar is pressed bird jumps
            bird.jump();
        }
    }
}

interface IImage {
    public ImageIcon loadImage();
}

// ProxyImage class is used to load the image of all the objects
class ProxyImage implements IImage {
    private final String src;
    private RealImage realImage;

    public ProxyImage(String src) {
        this.src = src;
    }

    public ImageIcon loadImage() {
        if(realImage == null) { // If the image has not been loaded
            this.realImage = new RealImage(src); // Load the image
        }
        return this.realImage.loadImage();
    }
}

class RealImage implements IImage {
    private final String src;
    private ImageIcon imageIcon;

    public RealImage(String src) {
        this.src = src;
    }

    @Override
    public ImageIcon loadImage() {
        if(imageIcon == null) {
            // Try to load from resources first
            try {
                this.imageIcon = new ImageIcon(getClass().getResource("/" + src));
            } catch (Exception e) {
                // Fallback to loading from file if resource not found
                System.out.println("Could not load image from resource: " + src);
                this.imageIcon = new ImageIcon(src);
            }
        }
        return imageIcon;
    }
}

// this class is used to create the window for the game
abstract class GameObject {
    protected int x, y;
    protected double dx, dy;
    protected int width, height;
    protected Image image;

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Image getImage() {
        return image;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setDx(double dx) {
        this.dx = dx;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public abstract void tick();
    public abstract void render(Graphics2D g, ImageObserver obs);
}

// this class is used to create the walls for the game
class Tube extends GameObject {
    private ProxyImage proxyImage;
    private int originalWidth;

    public Tube(int x, int y) {
        super(x, y);
        if (proxyImage == null) { // If the image has not been loaded
            proxyImage = new ProxyImage("TubeBody.png"); // Load the image
        }
        this.image = proxyImage.loadImage().getImage(); // Get the image
        this.originalWidth = image.getWidth(null); // save original width
        this.width = originalWidth; // set the width of the image
        this.height = image.getHeight(null); // set the height of the image
    }

    // Method to scale tube width while preserving aspect ratio
    public void scaleWidth(int newWidth) {
        if (originalWidth == 0) return;

        double scaleFactor = (double)newWidth / originalWidth;
        this.width = newWidth;
        // Note: We don't scale height here anymore as we'll set it directly
    }

    @Override
    public void tick() {
        this.x -= (int)dx; // Convert double dx to int for movement
    }

    @Override
    public void render(Graphics2D g, ImageObserver obs) {
        // Draw tube image, scaled to the specified width and height
        g.drawImage(image, x, y, width, height, obs);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

// this class is used to create the background for the game
class Game extends JPanel implements ActionListener {
    private boolean isRunning = false; // Variable used to check if the game is running
    private boolean isInMenu = true; // Added to track whether player is in menu or game
    private ProxyImage proxyImage; // Variable used to load the image
    private Image background; // Variable used to store the image
    private Bird bird; // Variable used to store the bird object
    private TubeColumn tubeColumn; // Variable used to store the wall object
    private int score;
    private int highScore;
    private Clip collisionSound; // Sound for collision with tubes
    private double difficultyFactor; // Global difficulty factor that increases over time
    private long gameStartTime; // Time when the game started
    private final long DIFFICULTY_INCREASE_INTERVAL = 10000; // Increase difficulty every 10 seconds

    // Add variables for bird selection
    private String selectedBirdImage = "bird.png"; // Default bird
    private boolean inBirdSelection = false; // Whether in bird selection screen
    private String[] availableBirds = {"bird.png", "bird2.png"}; // Available bird options
    private int selectedBirdIndex = 0; // Current selected bird index

    public Game() {
        proxyImage = new ProxyImage("background.jpg"); // Load the image
        background = proxyImage.loadImage().getImage(); // Get the image
        setFocusable(true);
        setDoubleBuffered(false);
        addKeyListener(new GameKeyAdapter());
        Timer timer = new Timer(15, this);
        timer.start();
        difficultyFactor = 1.0;

        // Load collision sound - direct file access
        try {
            File soundFile = new File("dang.wav");
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            collisionSound = AudioSystem.getClip();
            collisionSound.open(audioInputStream);
        } catch (Exception e) {
            System.out.println("Error loading collision sound: " + e.getMessage());
            createSilentClip();
        }
    }

    // Method to create a silent clip if sound loading fails
    private void createSilentClip() {
        try {
            // Create a 0.1-second silent audio format
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            byte[] silentData = new byte[4410]; // 0.1 second of silence
            AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(silentData),
                    format, silentData.length);

            collisionSound = AudioSystem.getClip();
            collisionSound.open(ais);
        } catch (Exception e) {
            System.out.println("Failed to create silent clip: " + e.getMessage());
        }
    }

    // Update difficulty based on elapsed time
    private void updateDifficulty() {
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        difficultyFactor = 1.0 + (elapsedTime / DIFFICULTY_INCREASE_INTERVAL) * 0.1;

        // Cap difficulty factor to prevent game from becoming impossible
        if (difficultyFactor > 1.7) {
            difficultyFactor = 1.7;
        }

        // Update difficulty for bird and tube columns
        if (bird != null) {
            bird.setDifficultyFactor(difficultyFactor);
        }

        if (tubeColumn != null) {
            tubeColumn.setDifficultyFactor(difficultyFactor);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Toolkit.getDefaultToolkit().sync(); // Synchronize the display on some systems
        if (isRunning) {
            updateDifficulty(); // Update game difficulty
            bird.tick(); // Update the bird
            tubeColumn.tick(); // Update the wall
            checkColision(); // Check if the bird has collided with the wall
            score++; // Increase the score by 1
        }
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Scale background to fill screen
        g2.drawImage(background, 0, 0, Window.WIDTH, Window.HEIGHT, null);

        if (isRunning) {
            // Game is running - draw game elements
            this.bird.render(g2, this);
            this.tubeColumn.render(g2, this);
            g2.setColor(Color.black);
            // Scale font sizes based on screen dimensions
            g.setFont(new Font("MV Boli", 1, Window.HEIGHT / 20));
            g2.drawString("Current score: " + this.tubeColumn.getPoints(), 10, 50);

            // Display current speed/difficulty
            String speedText = String.format("Speed: %.1f", this.tubeColumn.getSpeed());
            g2.drawString(speedText, 10, 100);
        } else if (inBirdSelection) {
            // Bird selection screen
            g2.setColor(Color.black);
            g.setFont(new Font("MV Boli", 1, Window.HEIGHT / 12));
            g2.drawString("Select Bird", Window.WIDTH / 2 - Window.WIDTH / 6, Window.HEIGHT / 4);

            // Draw bird options
            int startX = Window.WIDTH / 3;
            int startY = Window.HEIGHT / 2;
            int spacing = Window.WIDTH / 6;

            for (int i = 0; i < availableBirds.length; i++) {
                // Load and draw bird image
                ProxyImage birdImg = new ProxyImage(availableBirds[i]);
                Image img = birdImg.loadImage().getImage();

                // Calculate position to center the bird image
                int imgX = startX + (i * spacing) - img.getWidth(null) / 2;
                int imgY = startY - img.getHeight(null) / 2;

                // Draw the bird image
                g2.drawImage(img, imgX, imgY, null);

                // Draw selection box around currently selected bird
                if (i == selectedBirdIndex) {
                    g2.setColor(Color.BLUE);
                    g2.setStroke(new BasicStroke(3));
                    g2.drawRect(imgX - 10, imgY - 10,
                            img.getWidth(null) + 20, img.getHeight(null) + 20);
                }
            }

            // Draw instructions
            g2.setColor(Color.black);
            g.setFont(new Font("MV Boli", 1, Window.HEIGHT / 20));
            g2.drawString("← → to select    ENTER to confirm",
                    Window.WIDTH / 3, Window.HEIGHT * 3 / 4);
        } else {
            // Main menu
            g2.setColor(Color.black);
            // Scale font sizes based on screen dimensions
            g.setFont(new Font("MV Boli", 1, Window.HEIGHT / 12));
            g2.drawString("Press Enter to Start Game", Window.WIDTH / 2 - Window.WIDTH / 5, Window.HEIGHT / 2);

            g.setFont(new Font("MV Boli", 1, Window.HEIGHT / 20));
            g2.drawString("Press S to Select Bird", Window.WIDTH / 2 - Window.WIDTH / 7, Window.HEIGHT / 2 + 60);
        }

        g2.setColor(Color.black);
        g.setFont(new Font("MV Boli", 1, Window.HEIGHT / 20));
        g2.drawString("High Score: " + highScore, Window.WIDTH - Window.WIDTH / 4, 50);

        g.dispose();
    }

    private void restartGame() {
        if (!isRunning) {
            this.isRunning = true;
            this.isInMenu = false;
            this.inBirdSelection = false;
            this.difficultyFactor = 1.0; // Reset difficulty
            this.gameStartTime = System.currentTimeMillis(); // Record start time
            // Create bird with selected bird image
            this.bird = new Bird(Window.WIDTH / 2, Window.HEIGHT / 2, selectedBirdImage);
            this.tubeColumn = new TubeColumn(); // Create the wall object
        }
    }

    private void endGame() {
        this.isRunning = false;
        this.isInMenu = true;
        // Play collision sound when game ends
        playCollisionSound();

        if (this.tubeColumn.getPoints() > highScore) { // If the current score is higher than the high score
            this.highScore = this.tubeColumn.getPoints(); // Set the high score to the current score
        }
        this.tubeColumn.setPoints(0); // Set the current score to 0
    }

    // Show the bird selection screen
    private void showBirdSelection() {
        this.isInMenu = false;
        this.isRunning = false;
        this.inBirdSelection = true;
    }

    // Select previous bird
    private void selectPreviousBird() {
        selectedBirdIndex--;
        if (selectedBirdIndex < 0) {
            selectedBirdIndex = availableBirds.length - 1;
        }
        selectedBirdImage = availableBirds[selectedBirdIndex];
    }

    // Select next bird
    private void selectNextBird() {
        selectedBirdIndex++;
        if (selectedBirdIndex >= availableBirds.length) {
            selectedBirdIndex = 0;
        }
        selectedBirdImage = availableBirds[selectedBirdIndex];
    }

    // Confirm bird selection and return to main menu
    private void confirmBirdSelection() {
        this.inBirdSelection = false;
        this.isInMenu = true;
    }

    // Method to play the collision sound
    private void playCollisionSound() {
        if (collisionSound != null) {
            if (collisionSound.isRunning()) {
                collisionSound.stop();
            }
            collisionSound.setFramePosition(0);
            collisionSound.start();
        }
    }

    private void checkColision() {
        Rectangle rectBird = this.bird.getBounds(); // Get the bounds of the bird
        Rectangle rectTube; // Create a variable to store the bounds of the wall

        for (int i = 0; i < this.tubeColumn.getTubes().size(); i++) { // Loop through all the walls
            Tube tempTube = this.tubeColumn.getTubes().get(i); // Get the current wall
            rectTube = tempTube.getBounds(); // Get the bounds of the current wall
            if (rectBird.intersects(rectTube)) { // If the bird has collided with the wall
                endGame(); // End the game
            }
        }
    }

    class GameKeyAdapter extends KeyAdapter {
        private final Controller controller;

        public GameKeyAdapter() {
            controller = new Controller();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // Add escape key to exit fullscreen
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
            // Handle Enter key based on current screen
            else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (inBirdSelection) {
                    confirmBirdSelection();
                } else if (isInMenu) {
                    restartGame();
                }
            }
            // Enter bird selection with S key from main menu
            else if (e.getKeyCode() == KeyEvent.VK_S && isInMenu) {
                showBirdSelection();
            }
            // Handle bird selection navigation
            else if (inBirdSelection) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    selectPreviousBird();
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    selectNextBird();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (isRunning) {
                controller.controllerReleased(bird, e);
            }
        }
    }
}

class Window {
    // Width and height will be set to screen dimensions
    public static int WIDTH;
    public static int HEIGHT;

    public Window(int width, int height, String title, Game game) {
        JFrame frame = new JFrame();
        frame.add(game);
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the window when the user clicks the close button

        // Set to fullscreen
        frame.setUndecorated(true); // Remove window decorations
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize the window

        frame.setVisible(true);

        // Get actual dimensions after maximizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        WIDTH = screenSize.width;
        HEIGHT = screenSize.height;
    }

    // Run the application from here
    public static void main(String[] args) {
        // Set initial size to screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        WIDTH = screenSize.width;
        HEIGHT = screenSize.height;

        Game game = new Game();
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Window.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            Window window = new Window(WIDTH, HEIGHT, "Flappy Bird", game);
        });
    }
}
