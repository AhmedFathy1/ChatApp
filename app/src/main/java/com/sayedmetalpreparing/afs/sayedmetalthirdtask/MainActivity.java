package com.sayedmetalpreparing.afs.sayedmetalthirdtask;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private RecyclerView mRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView=findViewById(R.id.UsersListRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setProviders(providers).build(),RC_SIGN_IN);
        } else {
            Init();
        }

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,"Successfully signed in. Welcome!",Toast.LENGTH_LONG).show();
                Init();
            } else {
                Toast.makeText(this,"We couldn't sign you in. Please try again later.",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private String GetUserName(FirebaseUser user)
    {
        String displayName = user.getDisplayName();
        // If the above were null, iterate the provider data and set with the first non null data
        for (UserInfo userInfo : user.getProviderData())
        {
            if (displayName == null && userInfo.getDisplayName() != null) {
                displayName = userInfo.getDisplayName();
            }
        }
        return displayName;
    }

    private void AddUser()
    {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        ChatUser chatUser=new ChatUser(GetUserName(user),user.getUid());
        FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid()).setValue(chatUser);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(MainActivity.this,"You have been signed out.",Toast.LENGTH_LONG).show();
                    finish();
                }});}
        return true;
    }

    private void Init()
    {
        AddUser();
        String token= FirebaseInstanceId.getInstance().getToken();
        Log.d("token",token);
        FirebaseDatabase.getInstance().getReference("/fcmTokens/"+FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        DisplayUsers();
    }

    private void DisplayUsers()
    {
        final RecyclerView.Adapter usersAdapter=new FirebaseRecyclerAdapter<ChatUser,UserHolder>(ChatUser.class,R.layout.users_list_item,UserHolder.class, FirebaseDatabase.getInstance().getReference().child("Users")) {

            @Override
            protected void populateViewHolder(UserHolder viewHolder, ChatUser model, int position) {
                viewHolder.SetData(model);
                Log.d("Position", String.valueOf(position));
                Log.d("Position", model.mName+" : "+model.mUID);
            }

            @Override
            public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                Log.d("Creating", "View");
                return new UserHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.users_list_item, parent, false));

            }
        };

        // Scroll to bottom on new messages
        usersAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mRecyclerView.smoothScrollToPosition(usersAdapter.getItemCount());
                Log.d("Count", String.valueOf(usersAdapter.getItemCount()));
            }
        });

        mRecyclerView.setAdapter(usersAdapter);
    }

    private String GetChannelName(ChatUser chatUser)
    {
        String UID1=FirebaseAuth.getInstance().getCurrentUser().getUid();
        String UID2=chatUser.mUID;
        if (UID1.compareTo(UID2) < 0) return UID1+"_"+UID2; else return UID2+"_"+UID1;
    }

    private void OpenChannel(ChatUser chatUser)
    {
        String ChannelName=GetChannelName(chatUser);
        Intent intent=new Intent(this,ChatChannelActivity.class);
        intent.putExtra("ChannelName",ChannelName);
        startActivity(intent);
    }

    private class UserHolder extends RecyclerView.ViewHolder
    {
        TextView mUserName;
        ChatUser mUser;
        public UserHolder(View itemView) {
            super(itemView);
            mUserName=itemView.findViewById(R.id.UsersListItemUserName);
            mUser=new ChatUser();

            mUserName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("UserUUID",mUser.mUID);
                    OpenChannel(mUser);
                }
            });
        }

        public void SetData(ChatUser model)
        {
            mUser.mUID=model.mUID;
            mUser.mName=model.mName;
            mUserName.setText(model.mName);
        }

    }
}
