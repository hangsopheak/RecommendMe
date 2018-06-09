package pl.pollub.myrecommendation;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
import pl.pollub.myrecommendation.adapters.CommentRecyclerAdapter;
import pl.pollub.myrecommendation.adapters.NotificationRecyclerAdapter;
import pl.pollub.myrecommendation.models.Category;
import pl.pollub.myrecommendation.models.Comment;
import pl.pollub.myrecommendation.models.Notification;
import pl.pollub.myrecommendation.models.User;

public class CommentActivity extends AppCompatActivity {

    private FirebaseFirestore mFirestore;
    private FirebaseUser firebaseCurrentUser;
    private EditText etComment;
    private TextView tvPost;
    private CircleImageView ivProfilePicture;
    private RecyclerView rvComment;
    private CommentRecyclerAdapter commentRecyclerAdapter;
    private ArrayList<Comment> commentList;
    private User currentUser;
    private String recommendationId;
    private String recommendationUserId;
    private Set<String> commentUserIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        Intent intent = getIntent();
        recommendationId = intent.getStringExtra("recommendation_id");
        recommendationUserId = intent.getStringExtra("recommendation_user_id");

        commentUserIds = new HashSet<>();

        etComment = findViewById(R.id.etCommentInput);
        ivProfilePicture = findViewById(R.id.ivCommentProfilePicture);
        tvPost = findViewById(R.id.tvCommentPost);
        rvComment = findViewById(R.id.rvComment);

        mFirestore = FirebaseFirestore.getInstance();
        firebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(firebaseCurrentUser == null){
            sendToLogin();
            return;
        }
        currentUser = new User();
        currentUser.setId(firebaseCurrentUser.getUid());
        currentUser.setEmail(firebaseCurrentUser.getEmail());
        loadUserInfo();
        commentList = new ArrayList<>();
        commentRecyclerAdapter = new CommentRecyclerAdapter(commentList, currentUser.getId());
        rvComment.setLayoutManager(new LinearLayoutManager(CommentActivity.this));
        rvComment.setAdapter(commentRecyclerAdapter);

        tvPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertComment();
            }
        });

        loadComment();

        commentRecyclerAdapter.setOnItemClickListener(new CommentRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClickListener(View itemView, final int position) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                deleteComment(recommendationId, commentList.get(position).getId(), position);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(CommentActivity.this);
                builder.setMessage("Are you sure to delete this comment?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

            }
        });

    }

    private void loadUserInfo() {
        mFirestore.collection("Users").document(currentUser.getId()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()){
                    DocumentSnapshot data = task.getResult();
                    currentUser.setProfilePicture(data.get("profile_picture").toString());
                    RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
                    Glide.with(CommentActivity.this).applyDefaultRequestOptions(placeholderOption)
                            .load(currentUser.getProfilePicture()).into(ivProfilePicture);
                }
            }
        });
    }

    private void insertComment(){
        String comment = etComment.getText().toString();

        if(!TextUtils.isEmpty(comment)){
            etComment.setText("");
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("user_id", currentUser.getId());
            commentMap.put("content", comment);
            commentMap.put("timestamp", FieldValue.serverTimestamp());
            mFirestore.collection("Recommendation/" + recommendationId +"/comments").document().set(commentMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                           @Override
                           public void onComplete(@NonNull Task<Void> task) {
                               if(!task.isSuccessful()){
                                   String error = task.getException().getMessage();
                                   Toast.makeText(CommentActivity.this, "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                               }else{
                                   commentRecyclerAdapter.notifyDataSetChanged();
                                   // broadcast notification to recommendation poster and commentors
                                   if(!recommendationUserId.equals(currentUser.getId())){
                                       commentUserIds.add(recommendationUserId);
                                   }

                                   for(String commentUserId:commentUserIds){
                                       saveNotification(recommendationId, commentUserId);
                                   }
                               }
                           }
                       }
                    ) ;
        }

    }

    private void loadComment(){
        CollectionReference firstCollectionReference = mFirestore.collection("Recommendation/" + recommendationId + "/comments" );
        Query query = firstCollectionReference
                .orderBy("timestamp", Query.Direction.ASCENDING);

        query.addSnapshotListener(CommentActivity.this,new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if(doc.getType() == DocumentChange.Type.ADDED){
                        Comment comment = new Comment();
                        comment.setContent(doc.getDocument().getData().get("content").toString());
                        comment.setId(doc.getDocument().getId());
                        comment.setTimestamp((Date) doc.getDocument().getData().get("timestamp"));
                        comment.setUserId(doc.getDocument().getData().get("user_id").toString());
                        commentList.add(comment);
                        if(!comment.getUserId().equals(currentUser.getId())){
                            commentUserIds.add(comment.getUserId());
                        }
                    }
                }
                commentRecyclerAdapter.notifyDataSetChanged();
                rvComment.scrollToPosition(commentList.size() - 1);

            }
        });
    }

    private void sendToLogin(){
        Intent loginIntent = new Intent(CommentActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void saveNotification(String recommendationId, String userId){
        //if(userId.equals(currentUser.getId())) return;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("sender_id", currentUser.getId());
        notificationMap.put("recommendation_id", recommendationId);
        notificationMap.put("timestamp", FieldValue.serverTimestamp());
        notificationMap.put("unseen", true);
        notificationMap.put("type", 2);
        mFirestore.collection("Users/" + userId +"/notification").document().set(notificationMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                           @Override
                                           public void onComplete(@NonNull Task<Void> task) {
                                               if(!task.isSuccessful()){
                                                   String error = task.getException().getMessage();
                                                   Toast.makeText(CommentActivity.this, "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                                               }
                                           }
                                       }
                ) ;
    }

    private void deleteComment(String recommendationId, String commentId, final int position){
        mFirestore.collection("Recommendation/" + recommendationId + "/comments").document(commentId).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    String error =  task.getException().getMessage().toString();
                    Toast.makeText(CommentActivity.this, "Error Delete: " + error, Toast.LENGTH_LONG).show();
                }else{
                    commentList.remove(position);
                    commentRecyclerAdapter.notifyDataSetChanged();
                }
            }
        });
    }

}
