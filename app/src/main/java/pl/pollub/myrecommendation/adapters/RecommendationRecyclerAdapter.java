package pl.pollub.myrecommendation.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.models.Category;
import pl.pollub.myrecommendation.models.Recommendation;
import pl.pollub.myrecommendation.models.User;
import pl.pollub.myrecommendation.utils.MyUtil;

public class RecommendationRecyclerAdapter extends RecyclerView.Adapter<RecommendationRecyclerAdapter.ViewHolder> {

    public static final int DESCRIPTION_LENGTH = 230;
    public List<Recommendation> recommendationList;
    private Context mContext;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private OnItemClickListener listener;
    private String currentUserId;

    public RecommendationRecyclerAdapter(List<Recommendation> recommendationList, String currentUserId){
        this.recommendationList = recommendationList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public RecommendationRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.recommendation_item, parent, false);
        return new ViewHolder(view);
    }

    // Define listener member variable
    public interface OnItemClickListener{
        //void onItemClick(View itemView, int position);
        void onTitleClickListener(View itemView, int position);
        void onImageClickListener(View itemView, int position);
        void onSaveClickListener(View itemView, int position);
        void onCommentClickListener(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }


    @Override
    public void onBindViewHolder(@NonNull final RecommendationRecyclerAdapter.ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        holder.bindData(recommendationList.get(position));


    }



    @Override
    public int getItemCount() {
        return this.recommendationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView tvTitle, tvIdea, tvDescription, tvSaveCount,
                tvCommentCount, tvRecTime, tvCategoryName, tvUserName;
        private ImageView ivRecImage, ivRecUserProfilePicture, ivSave, ivComment;
        private View mView;

        public ViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;

            tvCategoryName = mView.findViewById(R.id.tvRecCategory);
            tvTitle = mView.findViewById(R.id.tvRecTitle);
            tvIdea = mView.findViewById(R.id.tvRecIdea);
            tvDescription = mView.findViewById(R.id.tvRecDescription);
            tvSaveCount = mView.findViewById(R.id.tvRecSaveCount);
            tvCommentCount = mView.findViewById(R.id.tvRecCommentCount);
            tvUserName = mView.findViewById(R.id.tvRecUserName);
            tvRecTime = mView.findViewById(R.id.tvRecPostTime);
            ivSave = mView.findViewById(R.id.ivRecSave);
            ivComment = mView.findViewById(R.id.ivRecComment);

            ivRecImage = mView.findViewById(R.id.ivRecImage);
            ivRecUserProfilePicture = mView.findViewById(R.id.ivRecProfilePicture);

            tvTitle.setOnClickListener(this);
            ivRecImage.setOnClickListener(this);
            ivSave.setOnClickListener(this);
            ivComment.setOnClickListener(this);

        }

        public void bindData(Recommendation recommendation){
            bindUser(recommendation.getUserId());
            bindCategory(recommendation.getCategoryId());
            bindCountSaveUser(recommendation.getId());
            bindCountComment(recommendation.getId());
            bindRecommendation(recommendation);

        }

        private void bindRecommendation(Recommendation recommendation){
            tvTitle.setText(recommendation.getTitle());
            if(TextUtils.isEmpty(recommendation.getIdea())) tvIdea.setVisibility(View.GONE);
            else tvIdea.setText(recommendation.getIdea());
            String description = recommendation.getDescription();
            if(description!= null && description.length() > DESCRIPTION_LENGTH){
                description = description.substring(0, DESCRIPTION_LENGTH) + "...";
            }
            tvDescription.setText(description);
            String strTimeAgo = null;
            if(recommendation.getTimstamp() != null){
                strTimeAgo = MyUtil.getTimeAgo(recommendation.getTimstamp().getTime(), mContext);
            }
            tvRecTime.setText(strTimeAgo);
            RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
            Glide.with(mContext).applyDefaultRequestOptions(placeholderOption)
                    .load(recommendation.getImageUri()).into(ivRecImage);


        }

        private void bindUser(String userId){
            mFireStore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.getResult().exists() && task.isSuccessful()){
                        DocumentSnapshot data = task.getResult();
                        tvUserName.setText(data.get("name").toString());
                        RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
                        Glide.with(mContext).applyDefaultRequestOptions(placeholderOption)
                                .load(Uri.parse(data.get("profile_picture").toString())).into(ivRecUserProfilePicture);
                    }
                }
            });
        }

        private void bindCategory(String categoryId){
            mFireStore.collection("categories").document(categoryId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.getResult().exists() && task.isSuccessful()){
                        DocumentSnapshot data = task.getResult();
                        tvCategoryName.setText(data.get("name").toString());
                    }
                }
            });
        }

        private void bindCountSaveUser(String recommendationId){
            // get likes

            mFireStore.collection("Recommendation/" + recommendationId + "/saved_users").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    int saveCountNumber = 0;
                    if(documentSnapshots != null){
                        saveCountNumber = documentSnapshots.size();
                    }
                    String saveCountLabel = Integer.toString(saveCountNumber);
                    if(saveCountNumber > 1)  saveCountLabel = saveCountLabel + " Saves";
                    else  saveCountLabel = saveCountLabel + " Save";
                    tvSaveCount.setText(saveCountLabel);
                }
            });

            mFireStore.collection("Recommendation/" + recommendationId + "/saved_users").document(currentUserId)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                            if(documentSnapshot.exists()){
                                ivSave.setImageDrawable(mContext.getDrawable(R.drawable.baseline_bookmark_black_36));
                            }else{
                                ivSave.setImageDrawable(mContext.getDrawable(R.drawable.baseline_bookmark_border_black_36));
                            }
                        }
                    });
        }

        private void bindCountComment(String recommendationId){
            // get comments
            mFireStore.collection("Recommendation/" + recommendationId + "/comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    int commentCount = 0;
                    if(documentSnapshots != null){
                        commentCount = documentSnapshots.size();
                    }
                    String commentCountLabel = Integer.toString(commentCount);
                    if(commentCount > 1)  commentCountLabel = commentCountLabel + " Comments";
                    else  commentCountLabel = commentCountLabel + " Comment";
                    tvCommentCount.setText(commentCountLabel);
                }
            });

            mFireStore.collection("Recommendation/" + recommendationId + "/saved_users").document(currentUserId)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                            if(documentSnapshot.exists()){
                                ivSave.setImageDrawable(mContext.getDrawable(R.drawable.baseline_bookmark_black_36));
                            }else{
                                ivSave.setImageDrawable(mContext.getDrawable(R.drawable.baseline_bookmark_border_black_36));
                            }
                        }
                    });
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.tvRecTitle:
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onTitleClickListener(itemView, position);
                        }
                    }
                    break;
                case R.id.ivRecImage:
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onImageClickListener(itemView, position);
                        }
                    }
                    break;
                case R.id.ivRecSave:
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onSaveClickListener(itemView, position);
                        }
                    }
                    break;
                case R.id.ivRecComment:
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onCommentClickListener(itemView, position);
                        }
                    }
                    break;
            }
        }
    }


}
