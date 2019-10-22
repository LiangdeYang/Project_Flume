package comp5216.sydney.edu.au.project_flume;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import comp5216.sydney.edu.au.project_flume.Adapter.MessageAdapter;
import comp5216.sydney.edu.au.project_flume.Fragments.APIService;
import comp5216.sydney.edu.au.project_flume.Model.Chat;
import comp5216.sydney.edu.au.project_flume.Model.User;
import comp5216.sydney.edu.au.project_flume.Notification.Client;
import comp5216.sydney.edu.au.project_flume.Notification.Data;
import comp5216.sydney.edu.au.project_flume.Notification.MyRespond;
import comp5216.sydney.edu.au.project_flume.Notification.NotificationSender;
import comp5216.sydney.edu.au.project_flume.Notification.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    ImageView targetUser_image;
    TextView userName_view;
    ImageButton sendBtn;
    EditText inputEditText;
    RecyclerView recyclerView;
    Button endChatBtn, settingBtn;
    FirebaseUser fUser;
    DatabaseReference targetUserRef, chatRef;
    Intent intent;

    Boolean notify = false;
    APIService apiService;
    String targetUserId;

    MessageAdapter messageAdapter;
    List<Chat> mChat;
    ValueEventListener seenListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //TODO check if both sides stay matching
        InitUI();

        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);

        GetTargetUser();
        SeenMessage();
    }

    private void InitUI() {

        //set up chat view
        LinearLayoutManager lManager = new LinearLayoutManager(getApplicationContext());
        lManager.setStackFromEnd(true);
        recyclerView = findViewById(R.id.recyclerView_chat);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(lManager);

        //set an empty adapter and update later.
        List<Chat> tempChat  = new ArrayList<>();
        MessageAdapter emptyAdapter = new MessageAdapter(ChatActivity.this, tempChat, "default");
        recyclerView.setAdapter(emptyAdapter);

        targetUser_image = findViewById(R.id.profile_image_chat);
        userName_view = findViewById(R.id.username_view_chat);
        sendBtn = findViewById(R.id.send_Btn_chat);
        inputEditText = findViewById(R.id.input_chat);
        endChatBtn = findViewById(R.id.endChatBtn_chat);
        settingBtn = findViewById(R.id.settingBtn_chat);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                String message = inputEditText.getText().toString();
                if(!message.equals("")) {
                    SendMessage(fUser.getUid(), targetUserId, message);
                }else{
                    Toast.makeText(getApplicationContext(), "message cannot be blank",
                            Toast.LENGTH_SHORT).show();
                }
                inputEditText.setText("");
            }
        });

        endChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EndChat();
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChatActivity.this, SettingActivity.class);
                i.putExtra("from", "Chat");
                startActivity(i);
            }
        });
    }
    //get the the target user info
    private void GetTargetUser() {

        try {
            intent = getIntent();
            targetUserId = intent.getStringExtra("targetId");
            fUser = FirebaseAuth.getInstance().getCurrentUser();
            targetUserRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(targetUserId);
            targetUserRef.addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    User user = dataSnapshot.getValue(User.class);
                    userName_view.setText(user.getUsername());

                    if(user.getImageUri().equals("default")) {
                        targetUser_image.setImageResource(R.mipmap.ic_launcher);
                    }
                    else{
                        Glide.with(ChatActivity.this).load(user.getImageUri())
                                .into(targetUser_image);
                    }
                    ReadMessage(fUser.getUid(), targetUserId, user.getImageUri());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

        }catch (Exception e) {

            Log.d("ChatActivity exception", e.toString());
            Toast.makeText(ChatActivity.this, "Sorry, Something goes wrong",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ChatActivity.this, MainActivity.class));
        }
    }

    //End the chat
    private void EndChat() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setTitle("End Chat")
                .setMessage("Are you sure to end chatting ?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        UnMatch();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Nothing happens
                    }});
        builder.create().show();
    }

    //UnMatch
    private void UnMatch() {
        try {
            //unMatch the target user
            DatabaseReference matchUserRef = FirebaseDatabase.getInstance()
                    .getReference("Users").child(targetUserId);
            matchUserRef.child("isMatch").setValue("N");
            matchUserRef.child("matchId").setValue("N");

            //unMatch the current user
            DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                    .getReference("Users").child(fUser.getUid());
            currentUserRef.child("isMatch").setValue("N");
            currentUserRef.child("matchId").setValue("N");

            //TODO handle matches fail, consider transaction
            startActivity(new Intent(ChatActivity.this, HomeActivity.class));

        }catch (Exception e) {

            Log.d("UnMatchUser Exception", e.toString());
            Toast.makeText(ChatActivity.this, "UnMatch fails",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //send message
    private void SendMessage(String sender, final String receiver, final String message) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> map = new HashMap<>();

        map.put("sender",sender);
        map.put("receiver",receiver);
        map.put("message",message);
        map.put("isSeen", false);

        ref.child("Chats").push().setValue(map);

        final String msg = message;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").
                child(fUser.getUid());
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(notify){
                    sendNotification(receiver, user.getUsername(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(String receiver, final String username, final String message){
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(fUser.getUid(), R.mipmap.ic_launcher,  username
                            + ": " + message, "New Message", targetUserId);

                    NotificationSender sender = new NotificationSender(data, token.getToken());

                    apiService.sendNotification(sender).enqueue(new Callback<MyRespond>() {
                        @Override
                        public void onResponse(Call<MyRespond> call, Response<MyRespond> response) {
                            if(response.code() == 200){
                                if(response.body().success != 1) {
                                }else{
                                    Toast.makeText(ChatActivity.this, "notification Failed",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<MyRespond> call, Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    //read message by sender and receiver
    private void ReadMessage(final String myId,final String targetId,final String imageURL) {

            mChat = new ArrayList<>();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats");
            ref.addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    mChat.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                        Chat chat = snapshot.getValue(Chat.class);

                        if((chat.getReceiver().equals(myId) && chat.getSender().equals(targetId))
                                || (chat.getReceiver().equals(targetId) && chat.getSender()
                                .equals(myId)) ){
                            mChat.add(chat);
                        }
                        messageAdapter = new MessageAdapter(ChatActivity.this,
                                mChat, imageURL);
                        recyclerView.setAdapter(messageAdapter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
    }

    private void SeenMessage() {
        chatRef = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = chatRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if(chat.getReceiver().equals(fUser.getUid()) && chat.getSender().equals(targetUserId)) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("isSeen", true);
                        snapshot.getRef().updateChildren(map);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        chatRef.removeEventListener(seenListener);
    }
}
