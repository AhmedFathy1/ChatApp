package com.sayedmetalpreparing.afs.sayedmetalthirdtask;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChatChannelActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    DatabaseReference mChannelRef;
    FloatingActionButton mFloatingActionButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_channel);

        mFloatingActionButton =(FloatingActionButton)findViewById(R.id.fab);
        mRecyclerView=findViewById(R.id.list_of_messages);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),layoutManager.getOrientation());

        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mRecyclerView.setLayoutManager(layoutManager);

        String ChannelName= getIntent().getStringExtra("ChannelName");


        mChannelRef=FirebaseDatabase.getInstance().getReference().child("Channels").child(ChannelName);

        init();
    }

    private void init()
    {
        final RecyclerView.Adapter chatAdapter=new FirebaseRecyclerAdapter<ChatMessage, ChatHolder>(ChatMessage.class,R.layout.message,ChatHolder.class,mChannelRef) {
            @Override
            protected void populateViewHolder(ChatHolder viewHolder, ChatMessage model, int position) {
                viewHolder.messageText.setText(model.getMessageText());
                viewHolder.messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",model.getMessageTime()));
                viewHolder.messageUser.setText(model.getMessageUser());
            }

            @Override
            public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ChatHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message, parent, false));
            }
        };

        // Scroll to bottom on new messages
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount());
            }
        });

        mRecyclerView.setAdapter(chatAdapter);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = (EditText)findViewById(R.id.input);
                // Read the input field and push a new instance of ChatMessage to the Firebase database
                mChannelRef.push().setValue(new ChatMessage(input.getText().toString(), FirebaseAuth.getInstance().getCurrentUser().getDisplayName()));
                // Clear the input
                input.setText("");
            }
        });
    }

    private class ChatHolder extends RecyclerView.ViewHolder
    {
        TextView messageText,messageUser,messageTime;
        public ChatHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageUser = itemView.findViewById(R.id.message_user);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }
}
