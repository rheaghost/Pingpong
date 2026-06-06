package com.example.pingpong;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class GameView extends SurfaceView implements Runnable {
    private static final int WINNING_SCORE = 10; // Score needed to win
    private int scoreLeft = 0, scoreRight = 0;
    private boolean isGameOver = false;

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isRunning;
    private Paint paint;
    private boolean isPaused = true; // Game starts paused

    // Game objects
    private int ballX, ballY; // Ball position
    private int ballSpeedX = 5, ballSpeedY = 5; // Ball speed
    private int paddleLeftY, paddleRightY; // Paddle positions
    private int paddleWidth = 30, paddleHeight = 150; // Paddle size

    // Sound related
    private SoundPool soundPool;
    private int soundPaddle, soundWall, soundScore, soundGameOver;
    private boolean isSoundEnabled = true; // Track sound state

    private Vibrator vibrator;

    // GameOverListener interface
    public interface GameOverListener {
        void onGameOver(boolean isLeftPlayerWinner);
    }

    private GameOverListener gameOverListener; // Listener instance
    // Add this method to set the listener
    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // Initialize common setup
    private void init(Context context) {
        if (!isInEditMode()) {
            holder = getHolder();
            paint = new Paint();
            resetBall();

            // SoundPool setup
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttributes)
                    .build();

            // Load sounds
            soundPaddle = soundPool.load(context, R.raw.paddle_hit, 1);
            soundWall = soundPool.load(context, R.raw.wall_hit, 1);
            soundScore = soundPool.load(context, R.raw.score, 1);
            soundGameOver = soundPool.load(context, R.raw.game_over, 1);

            // Vibrator
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    // Reset ball to center
    private void resetBall() {
        ballX = getWidth() / 2;
        ballY = getHeight() / 2;
        ballSpeedX = (Math.random() > 0.5) ? 5 : -5;
        ballSpeedY = (Math.random() > 0.5) ? 5 : -5;
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!holder.getSurface().isValid()) continue;

            if (!isPaused && !isGameOver) { // Only update/draw when NOT paused and NOT game over
                update();
                draw();
            }

            // Add slight delay (60 FPS)
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Update game state
    private void update() {
        if (!isGameOver) {
            ballX += ballSpeedX;
            ballY += ballSpeedY;

            // Ball collision with top/bottom
            if (ballY <= 0 || ballY >= getHeight()) {
                ballSpeedY *= -1;
                playSound(soundWall);
            }

            // Ball collision with paddles
            if (ballX - 20 <= paddleWidth && ballY + 20 >= paddleLeftY && ballY - 20 <= paddleLeftY + paddleHeight) {
                ballSpeedX = Math.abs(ballSpeedX);
                playSound(soundPaddle);
                vibratePhone();
            }
            if (ballX + 20 >= getWidth() - paddleWidth && ballY + 20 >= paddleRightY && ballY - 20 <= paddleRightY + paddleHeight) {
                ballSpeedX = -Math.abs(ballSpeedX);
                playSound(soundPaddle);
                vibratePhone();
            }

            // Scoring
            if (ballX <= 0) {
                scoreRight++;
                playSound(soundScore);
                resetBall();
            }
            if (ballX >= getWidth()) {
                scoreLeft++;
                playSound(soundScore);
                resetBall();
            }

            // AI Movement
            if (ballY > paddleRightY + paddleHeight / 2) {
                paddleRightY += 4;
            } else if (ballY < paddleRightY + paddleHeight / 2) {
                paddleRightY -= 4;
            }

            // Keep AI paddle within screen bounds
            if (paddleRightY < 0) paddleRightY = 0;
            if (paddleRightY > getHeight() - paddleHeight) {
                paddleRightY = getHeight() - paddleHeight;
            }

            // Check for game over
            checkGameOver();
        }
    }

    // Draw game objects
    private void draw() {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.BLACK);

        // Draw paddles
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, paddleLeftY, paddleWidth, paddleLeftY + paddleHeight, paint);
        canvas.drawRect(getWidth() - paddleWidth, paddleRightY, getWidth(), paddleRightY + paddleHeight, paint);

        // Draw ball
        canvas.drawCircle(ballX, ballY, 20, paint);

        // Draw scores
        paint.setTextSize(50);
        canvas.drawText(String.valueOf(scoreLeft), 100, 100, paint);
        canvas.drawText(String.valueOf(scoreRight), getWidth() - 100, 100, paint);

        // Draw "PAUSED" text if paused
        if (isPaused) {
            paint.setTextSize(100);
            paint.setColor(Color.WHITE);
            String pausedText = "PAUSED";
            float x = (getWidth() - paint.measureText(pausedText)) / 2;
            float y = getHeight() / 2;
            canvas.drawText(pausedText, x, y, paint);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    // Start/stop game loop
    public void resume() {
        isPaused = false;
        if (gameThread == null) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void pause() {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Toggle pause state
    public void togglePause() {
        isPaused = !isPaused;
    }

    // Check if game is paused
    public boolean isPaused() {
        return isPaused;
    }

    // Set sound enabled/disabled
    public void setSoundEnabled(boolean enabled) {
        isSoundEnabled = enabled;
    }

    // Play sound if enabled
    private void playSound(int soundID) {
        if (isSoundEnabled && soundPool != null) {
            soundPool.play(soundID, 1, 1, 0, 0, 1);
        }
    }

    // Vibrate phone
    private void vibratePhone() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50); // Legacy method
            }
        }
    }

    // Check for game over
    private void checkGameOver() {
        if (scoreLeft >= WINNING_SCORE || scoreRight >= WINNING_SCORE) {
            isGameOver = true;
            isRunning = false; // Stop the game loop
            if (gameOverListener != null) {
                Log.d("GameView", "Calling onGameOver"); // Debug log
                ((Activity) getContext()).runOnUiThread(() ->
                        gameOverListener.onGameOver(scoreLeft >= WINNING_SCORE)
                );
            } else {
                Log.d("GameView", "GameOverListener is null"); // Debug log
            }
        }
    }

    // Reset game state
    public void resetGame() {
        scoreLeft = 0;
        scoreRight = 0;
        resetBall();
        isGameOver = false;
        isPaused = true; // Reset to paused state

        // Stop the game loop
        if (gameThread != null) {
            isRunning = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gameThread = null;
        }

        // Restart the game loop
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // Release resources
    public void release() {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    // Handle touch events
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        paddleLeftY = (int) event.getY() - paddleHeight / 2;
        if (paddleLeftY < 0) paddleLeftY = 0;
        if (paddleLeftY > getHeight() - paddleHeight) paddleLeftY = getHeight() - paddleHeight;
        return true;
    }

    //retrieve the scores of each
    public int getScoreLeft(){
        return scoreLeft;
    }

    public int getScoreRight()
    {
        return scoreRight;

    }
}
