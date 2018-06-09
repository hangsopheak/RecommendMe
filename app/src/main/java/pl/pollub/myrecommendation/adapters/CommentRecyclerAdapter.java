package pl.pollub.myrecommendation.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.models.Comment;
import pl.pollub.myrecommendation.utils.MyUtil;

public class CommentRecyclerAdapter extends RecyclerView.Adapter<CommentRecyclerAdapter.ViewHolder> {

    public List<Comment> commentList;
    private Context mContext;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private OnItemClickListener listener;
    private String currentUserId;

    public CommentRecyclerAdapter(List<Comment> commentList, String currentUserId){
        this.commentList = commentList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public CommentRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.comment_item, parent, false);
        return new ViewHolder(view);
    }

    // Define listener member variable
    public interface OnItemClickListener{
        void onDeleteClickListener(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }


    @Override
    public void onBindViewHolder(@NonNull final CommentRecyclerAdapter.ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        holder.bindData(commentList.get(position));


    }



    @Override
    public int getItemCount() {
        return this.commentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView ivCommentProfilePicture, ivCommentDelete;
        private TextView tvCommentUserName, tvCommentContent, tvCommentTimeAgo;
        private View mView;
        private CardView cvComment;

        public ViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;

            ivCommentProfilePicture = mView.findViewById(R.id.ivCommentItemProfilePicture);
            ivCommentDelete = mView.findViewById(R.id.ivCommentDelete);
            tvCommentUserName = mView.findViewById(R.id.tvCommentItemUserName);
            tvCommentContent = mView.findViewById(R.id.tvCommentItemContent);
            tvCommentTimeAgo = mView.findViewById(R.id.tvCommentItemTimeAgo);
            cvComment = mView.findViewById(R.id.cvComment);

            ivCommentDelete.setOnClickListener(this);

        }

        public void bindData(Comment comment){
            bindUser(comment.getUserId());
            bindComment(comment);

        }

        private void bindComment(Comment comment) {
            tvCommentContent.setText(comment.getContent());
            String strTimeAgo = "just now";
            if(comment.getTimestamp() != null){
                strTimeAgo = MyUtil.getTimeAgo(comment.getTimestamp().getTime(), mContext);

            }
            tvCommentTimeAgo.setText(strTimeAgo);
            if(comment.getUserId().equals(currentUserId)){
                ivCommentDelete.setVisibility(View.VISIBLE);
            }else{
                ivCommentDelete.setVisibility(View.GONE);
            }

        }


        private void bindUser(String userId){
            mFireStore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.getResult().exists() && task.isSuccessful()){
                        DocumentSnapshot data = task.getResult();
                        tvCommentUserName.setText(data.get("name").toString());
                        RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
                        Glide.with(mContext).applyDefaultRequestOptions(placeholderOption)
                                .load(Uri.parse(data.get("profile_picture").toString())).into(ivCommentProfilePicture);
                    }
                }
            });
        }


        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.ivCommentDelete:
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onDeleteClickListener(itemView, position);
                        }
                    }
                    break;
            }
        }
    }


}
