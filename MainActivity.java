
//code now works but ball does not correctly bounce when at screen left top
//2501290744

//1ok 2501301936
package com.example.pingpong;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pingpong.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements GameView.GameOverListener {
    private ActivityMainBinding binding;
    private GameView gameView;
    private boolean isGameStarted = false;
    private boolean isSoundOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize GameView
        gameView = binding.gameView;
        gameView.setGameOverListener(this); // Set the listener

        // Set initial button text
        binding.btnPause.setText("Start");
        binding.btnSound.setText("Sound: On");

        // Pause/Resume button click listener
        binding.btnPause.setOnClickListener(v -> {
            if (!isGameStarted) {
                isGameStarted = true;
                gameView.resume(); // Start the game loop
                binding.btnPause.setText("Pause"); // Change button text
            } else {
                gameView.togglePause();
                binding.btnPause.setText(gameView.isPaused() ? "Resume" : "Pause");
            }
        });

        // Sound toggle button click listener
        binding.btnSound.setOnClickListener(v -> {
            isSoundOn = !isSoundOn; // Toggle sound state
            gameView.setSoundEnabled(isSoundOn); // Update sound state in GameView
            binding.btnSound.setText(isSoundOn ? "Sound: On" : "Sound: Off");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGameStarted) {
            gameView.resume(); // Resume the game if it was started
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGameStarted) {
            gameView.pause(); // Pause the game if it was started
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameView.release(); // Clean up sounds
    }

    // Implement the onGameOver method
    @Override
    public void onGameOver(boolean isLeftPlayerWinner) {
        Log.d("MainActivity", "onGameOver called"); // Debug log
        showGameOver(isLeftPlayerWinner);
    }

    // Add this method to show the Game Over screen
    public void showGameOver(boolean isLeftPlayerWinner) {
        Log.d("MainActivity", "showGameOver called"); // Debug log

        // Inflate the Game Over layout
        View gameOverView = getLayoutInflater().inflate(R.layout.game_over, null);

        // Set the winner text
        TextView tvWinner = gameOverView.findViewById(R.id.tvWinner);
        tvWinner.setText(isLeftPlayerWinner ? "Player 1 Wins!" : "Player 2 Wins!");

        // Set the final scores
        TextView tvFinalScores = gameOverView.findViewById(R.id.tvFinalScores);
        tvFinalScores.setText("Final Scores: " + gameView.getScoreLeft() + " - " + gameView.getScoreRight());


        // Set up the Play Again button
        Button btnPlayAgain = gameOverView.findViewById(R.id.btnPlayAgain);
        btnPlayAgain.setOnClickListener(v -> {
            gameView.resetGame();
            ((ViewGroup) gameOverView.getParent()).removeView(gameOverView); // Remove the Game Over screen
            binding.btnPause.setText("Start"); // Reset the pause/resume button text
            isGameStarted = false; // Reset game state
        });

        // Set up the Quit button
        Button btnQuit = gameOverView.findViewById(R.id.btnQuit);
        btnQuit.setOnClickListener(v -> {
            finish(); // Close the activity and exit the game
        });


        // Add the Game Over screen to the root view
        ViewGroup rootView = (ViewGroup) binding.getRoot();
        rootView.addView(gameOverView);
        gameOverView.bringToFront(); // Bring the Game Over layout to the front
        //Log.d("MainActivity", "Game Over layout added to root view"); // Debug log

        // Apply fade-in  and slide-in animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        gameOverView.startAnimation(fadeIn);
        gameOverView.startAnimation(slideIn);
        //Log.d("MainActivity", "Fade-in animation started"); // Debug log
        Log.d("MainActivity", "Game Over layout added to root view"); // Debug log
    }

}
