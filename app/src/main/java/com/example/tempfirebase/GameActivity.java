package com.example.tempfirebase;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends Activity implements View.OnClickListener  {



    TextView txt_Turn;
    TextView TimerText;
    TextView greenTextview;
    TextView redTextview;
    Button btn_Test;
    Button btn_Undo;
    Button btn_Restart;
    Button btnAlarm;
    private int rows = 6 ;
    private int columns =  7;
    private GameEngine gE;
    private int buttons[] = {R.drawable.red_t, R.drawable.green_t};
    private String[] s_Turns = {"Red", "Green"};
    private int win_buttons[] = { R.drawable.red_wint,R.drawable.green_wint};
    private int temp_buttons[] ={ R.drawable.empty_t,R.drawable.green_t, R.drawable.red_t};
    private  int GreenWinCount = 0;
    private  int RedWinCount = 0;
    private  int  moveCounter = 0;


    FirebaseDatabase db = FirebaseDatabase.getInstance("https://connect4-75100-default-rtdb.firebaseio.com/");



    GridView gridview;
    int seconds, minutes, milliSeconds;
    long millisecondTime, startTime, timeBuff, updateTime = 0L ;
    Handler handler;

    DatabaseReference scoreRef = db.getReference("score");


    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            millisecondTime = SystemClock.uptimeMillis() - startTime;
            updateTime = timeBuff + millisecondTime;
            seconds = (int) (updateTime / 1000);
            minutes = seconds / 60;
            seconds = seconds % 60;
            milliSeconds = (int) (updateTime % 1000);

            TimerText.setText(MessageFormat.format("{0}:{1}:{2}", minutes, String.format(Locale.getDefault(), "%02d", seconds), String.format(Locale.getDefault(),"%01d", milliSeconds)));
            handler.postDelayed(this, 0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        setContentView(R.layout.activity_game);
        txt_Turn = (TextView) findViewById(R.id.txt_Turn);
        btn_Test = (Button)findViewById(R.id.btn_Test);
        btn_Undo = (Button) findViewById(R.id.btn_Undo);
        btn_Restart = (Button) findViewById(R.id.btn_Restart);
        greenTextview = (TextView) findViewById(R.id.GreenTextView);
        redTextview = (TextView) findViewById(R.id.RedTextView);
        TimerText = (TextView)findViewById(R.id.TimerText);
        handler = new Handler(Looper.getMainLooper());
        btnAlarm =(Button) findViewById(R.id.btnAlarm);


        btn_Test.setOnClickListener(this);
        btn_Test.setVisibility(View.GONE);
        btn_Undo.setOnClickListener(this);
        btn_Restart.setOnClickListener(this);


        gridview = (GridView) findViewById(R.id.gridview);
        gridview.setNumColumns(7);
        gridview.setAdapter(new ImageAdapter(this, rows*columns));
        txt_Turn.setText(s_Turns[0]);


        gE = new GameEngine(rows,columns);


        creatNotificationChannel();


        btnAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(GameActivity.this, "Reminder Set", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(GameActivity.this, ReminderBroadcast.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(GameActivity.this,0,intent, PendingIntent.FLAG_IMMUTABLE);

                AlarmManager alarmManager =(AlarmManager) getSystemService(ALARM_SERVICE);
                long timeAtButtonClick = System.currentTimeMillis();
                long tenSecondsInMillis = 1000 * 10;

                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        timeAtButtonClick + tenSecondsInMillis, pendingIntent);

            }
        });



        startTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);




        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if (!gE.finished) {
                    int addPosition = gE.addToColumn(position);
                    txt_Turn.setText(s_Turns[(gE.getCount() + 1) % 2]);
                    if (addPosition < rows * columns) {
                        moveCounter++;
                        ImageView chosen = (ImageView) parent.getChildAt(addPosition);
                        chosen.setImageResource(buttons[getTurn()]);

                        if (gE.finished) {
                            if (!gE.win.isEmpty()) {
                                for (int i : gE.win) {
                                    chosen = (ImageView) parent.getChildAt(i);
                                    chosen.setImageResource(win_buttons[getTurn()]);
                                }

                                timeBuff += millisecondTime;
                                handler.removeCallbacks(runnable);

                                if (s_Turns[getTurn()]=="Green") {
                                    GreenWinCount++;
                                    greenTextview.setText("Green Counter: " + (GreenWinCount / 4));
                                    checkAndUpdateScore(moveCounter / 2);
                                } else if (s_Turns[getTurn()]=="Red") {
                                    RedWinCount++;
                                    redTextview.setText("Red Counter: " + (RedWinCount / 4));
                                    checkAndUpdateScore((moveCounter + 1) / 2);
                                }

                                Toast.makeText(GameActivity.this, "Game Over " + s_Turns[getTurn()] + " Won", Toast.LENGTH_SHORT).show();
                                btn_Undo.setEnabled(false);
                            } else {
                                Toast.makeText(GameActivity.this, "Game Over Nobody Won", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });


        TimerText.setText("00:00:00");


    }
    private void checkAndUpdateScore(int currentScore) {
        scoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer bestScore = dataSnapshot.getValue(Integer.class);
                if (bestScore == null || currentScore < bestScore) {
                    Toast.makeText(GameActivity.this, "Hey the current scores is " + currentScore + " moves!!!", Toast.LENGTH_SHORT).show();

                    scoreRef.setValue(currentScore).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(GameActivity.this, "You broke the record with a win in " + currentScore + " moves!!!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GameActivity.this, "Failed to update the score.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException());
            }
        });
    }


    private void creatNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "COMETOPLAY";
            String description = "Channel for Come To Play";
            int impotance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("COMETOPLAY", name, impotance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public void onClick(View v) {


        if(v.getId()== R.id.btn_Test){
            placeButtons();
        }
        else if(v.getId()== R.id.btn_Undo){
            btn_Undo.setEnabled(true);
            unDo();
        }
        else if(v.getId()== R.id.btn_Restart){


            millisecondTime = 0L ;
            startTime = 0L ;
            timeBuff = 0L ;
            updateTime = 0L ;
            seconds = 0 ;
            minutes = 0 ;
            milliSeconds = 0 ;
            TimerText.setText("00:00:00");






            txt_Turn.setText(s_Turns[0]);
            btn_Undo.setEnabled(true);


            gE.restart();
            for(int i = 0; i<rows*columns; i++){
                ImageView l = (ImageView) gridview.getChildAt(i);
                l.setImageResource(R.drawable.empty_t);


                moveCounter=0;


                startTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);


            }

        }


    }

    public void unDo(){
        if(gE.getCount()>-1){
            int pos = gE.unDo();
            ImageView v = (ImageView) gridview.getChildAt(pos);
            v.setImageResource(R.drawable.empty_t);
            moveCounter--;

        }

    }


    public void placeButtons(){
        int[][]b= { { 2, 1, 2, 1, 2, 1, 1, },{ 1, 1, 1, 2, 1, 1, 1, },{ 2, 1, 2, 1, 1, 1, 1, },{ 1, 1, 1, 1, 1, 1, 1 },{ 1, 1,1, 1, 1, 1, 1 },{ 1, 1, 1, 1, 1, 1, 0}};
        gE.setBoard(b,0);
        for(int i = 0; i<rows;i++){
            for(int j = 0; j<columns; j++){
                ImageView temp = (ImageView) gridview.getChildAt(gE.getPosition(i,j));
                temp.setImageResource(temp_buttons[b[i][j]]);

            }
            System.out.println("");
        }

    }

    private int getTurn(){
        return gE.getCount()%2;
    }


}