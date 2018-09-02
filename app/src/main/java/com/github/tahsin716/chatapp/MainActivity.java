package com.github.tahsin716.chatapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * <p>The MainActivity first checks whether or not any user is signed in,
 * if not, it displays the RegisterActivity else the MainActivity is displayed.</p>
 *
 *  <p>If the user is logged in, it shows the messages in a FireBase RecyclerView which is binded to the android's
 *  default RecyclerView. And also text messages can be sent, which are stored in FireBase's realtime database.</p>
 *
 *  @author Tahsin Rashad
 *  @version 1.0.0
 *  @since 2018-07-15
 */
public class MainActivity extends AppCompatActivity {

    private EditText editMessage; //For the Text Message
    private DatabaseReference mDatabase; //For getting a reference to the database
    private Toolbar toolbar; //For getting a reference to the toolbar
    private RecyclerView mMessageList; //Reference to the RecyclerView
    private FirebaseAuth mAuth; //Reference to FireBaseAuth for authentication of user
    private FirebaseAuth.AuthStateListener mAuthListener; //Listener Class for authentication
    private FirebaseUser mCurrentUser; //Current user being registered in the database
    private DatabaseReference mDatabaseUsers; //Reference for all the users in the database


    /**
     * <p>All the class variables are initialized, which are: editMessage, mDatabase, toolbar,
     * mMessageList, mAuth, mAuthlistener, mcurrentUser and mDataBaseUsers.</p>
     *
     * <p>mAuthListener checks whether the user is signed in or not, if not, RegisterActivity class
     * is started.</p>
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);

        editMessage = (EditText) findViewById(R.id.editMessageE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Messages");

        mMessageList = (RecyclerView) findViewById(R.id.messageRec);
        mMessageList.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        mMessageList.setLayoutManager(linearLayoutManager);
        mAuth = FirebaseAuth.getInstance();

        //If user is not signed in, the RegisterActivity class is started.
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() ==  null){
                    startActivity(new Intent(MainActivity.this,RegisterActivity.class));
                }
            }
        };
    }

    /**
     * <p>A new message is sent, which is updated on the display as well as the RealTime database.
     * Firstly, the current user and all the users in the database are retrieved, along with the message,
     * that the user sent.</p>
     *
     * <p>If the message is not empty, a database reference for the new post is created. which is then populated. With
     * the child content for messageValue, and username of the sender which is retrieved from the DataSnapShot</p>
     * @param view
     */
    public void sendButtonClicked(View view ){

        mCurrentUser = mAuth.getCurrentUser(); //Current user
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid()); //All users reference
        final String messageValue = editMessage.getText().toString().trim(); //The message that has been sent.

        //If the message is not empty
        if (!TextUtils.isEmpty(messageValue)){

            final DatabaseReference newPost =  mDatabase.push();
            mDatabaseUsers.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    newPost.child("content").setValue(messageValue);
                    newPost.child("username").setValue(dataSnapshot.child("Name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                        }
                    });
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            mMessageList.scrollToPosition(mMessageList.getAdapter().getItemCount());
        }
    }

    /**
     * <p>If the user is signed in, during the start of the MainActivity class, the FireBaseRecyclerAdapter
     * will retrieve all the text messages from FireBase and display them in a RecyclerView</p>
     */
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        FirebaseRecyclerAdapter<Message,MessageViewHolder> FBRA = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(
                Message.class,
                R.layout.single_message,
                MessageViewHolder.class,
                mDatabase
        ) {
            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, Message model, int position) {
                viewHolder.setContent(model.getContent());
                viewHolder.setUsername(model.getUsername());
            }
        };
        mMessageList.setAdapter(FBRA);
    }


    public static class MessageViewHolder extends RecyclerView.ViewHolder{
        View mView;
        public MessageViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }
        public void setContent(String content){
            TextView message_content = (TextView) mView.findViewById(R.id.messageText);
            message_content.setText(content);
        }
        public void setUsername(String username){
            TextView username_content = (TextView) mView.findViewById(R.id.usernameText);
            username_content.setText(username);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //For signing out
        if (id == R.id.action_settings) {
            FirebaseAuth.getInstance().signOut();
        }

        return super.onOptionsItemSelected(item);
    }
}
