package com.example.audioapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int SPEECH_RECOGNITION_REQUEST_CODE = 88;

    private TextView textView;
    private Button send;
    private StringBuilder accumulatedText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        Button start = findViewById(R.id.start);
        send = findViewById(R.id.send);

        checkAndRequestPermissions();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Speak Now", Toast.LENGTH_SHORT).show();
                speak(v);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = accumulatedText.toString();
                if (!message.isEmpty()) {
                    sendToWhatsApp(message);
                    // Clear accumulated text after sending
                    accumulatedText.setLength(0);
                    textView.setText("");  // Clear the displayed text
                } else {
                    Toast.makeText(MainActivity.this, "No text to send", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void speak(View view) {
        if (isSpeechRecognitionAvailable()) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now");
            startActivityForResult(intent, SPEECH_RECOGNITION_REQUEST_CODE);
        } else {
            Log.d("SpeechRecognition", "Speech recognition not available on this device");
        }
    }

    private boolean isSpeechRecognitionAvailable() {
        return getPackageManager().resolveActivity(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0) != null;
    }

    private void checkAndRequestPermissions() {
        requestRecordAudioPermission();
        // Add other permissions if needed
    }

    private void requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now initiate speech recognition
                Log.d("PermissionDebug", "RECORD_AUDIO permission granted");
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
                Log.d("PermissionDebug", "RECORD_AUDIO permission denied");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("SpeechRecognition", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == SPEECH_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (results != null && !results.isEmpty()) {
                    String recognizedText = results.get(0);
                    Log.d("SpeechRecognition", "Recognized text: " + recognizedText);

                    // Append recognized text to accumulatedText
                    accumulatedText.append(recognizedText).append(" ");

                    // Display accumulated text in the TextView
                    textView.setText(accumulatedText.toString());
                } else {
                    Log.d("SpeechRecognition", "No results received");
                }
            } else {
                Log.d("SpeechRecognition", "Speech recognition canceled or failed");
            }
        }
    }

    private void sendToWhatsApp(String message) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");

        // Specify the class name of the WhatsApp activity
        sendIntent.setClassName("com.whatsapp", "com.whatsapp.ContactPicker");

        if (isWhatsAppInstalled()) {
            try {
                startActivity(sendIntent);
            } catch (Exception e) {
                // Handle exceptions specific to starting the WhatsApp activity
                Toast.makeText(MainActivity.this, "Failed to open WhatsApp", Toast.LENGTH_SHORT).show();
            }
        } else {
            // WhatsApp is not installed, open the WhatsApp chat screen using a URL
            openWhatsAppChatScreen(message);
        }
    }

    private boolean isWhatsAppInstalled() {
        try {
            getPackageManager().getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openWhatsAppChatScreen(String message) {
        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
        String url = "https://api.whatsapp.com/send?text=" + Uri.encode(message);
        sendIntent.setData(Uri.parse(url));

        try {
            startActivity(sendIntent);
        } catch (ActivityNotFoundException e) {
            // If WhatsApp is not available, handle the exception
            Toast.makeText(MainActivity.this, "WhatsApp not found", Toast.LENGTH_SHORT).show();
        }
    }

}
