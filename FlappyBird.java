import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int BoardWidth = 360;
    int BoardHeight = 640;

    // images
    Image backGroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // bird
    int BirdX = BoardWidth / 8;
    int BirdY = BoardHeight / 2;
    int BirdWidth = 34;
    int BirdHeight = 24;

    // game variables
    int score = 0;
    int highScore = 0;
    boolean gameOver = false;
    boolean gameStarted = false;
    long startTime = 0;
    long elapsedTime = 0;

    // sound effects
    Clip flapSound;
    Clip pointSound;
    Clip hitSound;
    Clip gameMusic;
    boolean soundsLoaded = false;

    // bird class
    class Bird {
        int X = BirdX;
        int Y = BirdY;
        int Width = BirdWidth;
        int Height = BirdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // pipes
    int pipeX = BoardWidth;
    int pipeY = 0;
    int pipeWidth = 64;
    int pipeHeight = 512;
    int pipeGap = 150;
    Random random = new Random();

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img, int y) {
            this.img = img;
            this.y = y;
        }
    }

    // game logic
    Bird bird;
    int velocityX = -4; // pipe speed
    int velocityY = 0; // bird speed
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Timer gameLoop;
    Timer placePipesTimer;
    Timer gameTimer;

    FlappyBird() {
        setPreferredSize(new Dimension(BoardWidth, BoardHeight));
        setFocusable(true);
        addKeyListener(this);

        // Load images
        try {
            backGroundImg = new ImageIcon(getClass().getResource("./Resources/Images/flappybirdbg.png")).getImage();
            birdImg = new ImageIcon(getClass().getResource("./Resources/Images/flappybird.png")).getImage();
            topPipeImg = new ImageIcon(getClass().getResource("./Resources/Images/toppipe.png")).getImage();
            bottomPipeImg = new ImageIcon(getClass().getResource("./Resources/Images/bottompipe.png")).getImage();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Images not found, using colored rectangles instead");
        }


        try {
            // Background music
            gameMusic = AudioSystem.getClip();
            gameMusic.open(AudioSystem.getAudioInputStream(
                    getClass().getResource("/Resources/Sounds/game-music.wav")));
            gameMusic.loop(Clip.LOOP_CONTINUOUSLY);

            soundsLoaded = true;
            System.out.println("All sounds loaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Sound loading error: " + e.getMessage());
            System.out.println("Continuing without sound");
        }

        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // Game timer to update elapsed time
        gameTimer = new Timer(1000, e -> {
            if (gameStarted && !gameOver) {
                elapsedTime = System.currentTimeMillis() - startTime;
            }
        });
        gameTimer.start();

        // Pipe placement timer
        placePipesTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameStarted && !gameOver) {
                    placePipes();
                }
            }
        });
        placePipesTimer.start();

        // Main game loop
        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // draw background
        if (backGroundImg != null) {
            g.drawImage(backGroundImg, 0, 0, BoardWidth, BoardHeight, null);
        } else {
            g.setColor(Color.cyan);
            g.fillRect(0, 0, BoardWidth, BoardHeight);
        }

        // draw pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            if (pipe.img != null) {
                g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
            } else {
                g.setColor(Color.green.darker());
                g.fillRect(pipe.x, pipe.y, pipe.width, pipe.height);
            }
        }

        // draw bird
        if (bird.img != null) {
            g.drawImage(bird.img, bird.X, bird.Y, bird.Width, bird.Height, null);
        } else {
            g.setColor(Color.red);
            g.fillRect(bird.X, bird.Y, bird.Width, bird.Height);
        }

        // draw score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString(String.valueOf(score), BoardWidth / 2 - 15, 50);

        // draw timer
        if (gameStarted) {
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            String timeString = formatTime(elapsedTime);
            g.drawString(timeString, 10, 30);
        }

        // game over or start message
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Game Over", BoardWidth / 2 - 100, BoardHeight / 2 - 50);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Press SPACE to restart", BoardWidth / 2 - 100, BoardHeight / 2);

            // draw high score and time played
            g.drawString("High Score: " + highScore, BoardWidth / 2 - 80, BoardHeight / 2 + 50);
            g.drawString("Time: " + formatTime(elapsedTime), BoardWidth / 2 - 80, BoardHeight / 2 + 80);
        } else if (!gameStarted) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Flappy Bird", BoardWidth / 2 - 100, BoardHeight / 2 - 50);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Press SPACE to start", BoardWidth / 2 - 100, BoardHeight / 2);
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void move() {
        if (!gameStarted || gameOver) return;

        // bird movement
        velocityY += gravity;
        bird.Y += velocityY;
        bird.Y = Math.max(bird.Y, 0); // don't go above screen

        // pipe movement
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            // score calculation
            if (!pipe.passed && pipe.x + pipe.width < bird.X) {
                pipe.passed = true;
                score++;
                if (score > highScore) {
                    highScore = score;
                }
            }

            // collision detection
            if (bird.X + bird.Width > pipe.x &&
                    bird.X < pipe.x + pipe.width &&
                    bird.Y < pipe.y + pipe.height &&
                    bird.Y + bird.Height > pipe.y) {
                gameOver();
            }

            // remove off-screen pipes
            if (pipe.x + pipe.width < 0) {
                pipes.remove(pipe);
                i--;
            }
        }

        // check if bird hit the ground
        if (bird.Y + bird.Height >= BoardHeight) {
            gameOver();
        }
    }

    private void gameOver() {
        gameOver = true;
        if (soundsLoaded) {
            gameMusic.stop();
        }
    }

    public void placePipes() {
        int randomPipeY = -(random.nextInt(pipeHeight / 4) + pipeHeight / 6);

        Pipe topPipe = new Pipe(topPipeImg, randomPipeY);
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg, randomPipeY + pipeHeight + pipeGap);
        pipes.add(bottomPipe);
    }

    public void resetGame() {
        bird.Y = BoardHeight / 2;
        velocityY = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
        gameStarted = false;
        elapsedTime = 0;
        if (soundsLoaded) {
            gameMusic.setFramePosition(0);
            gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void startGame() {
        gameStarted = true;
        startTime = System.currentTimeMillis();
        if (soundsLoaded) {
            gameMusic.setFramePosition(0);
            gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (gameOver) {
                resetGame();
            } else if (!gameStarted) {
                startGame();
            } else {
                velocityY = -8; // jump
                if (soundsLoaded) {
                    flapSound.setFramePosition(0);
                    flapSound.start();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}