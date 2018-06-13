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
import pl.pollub.myrecommendation.models.Notification;
import pl.pollub.myrecommendation.models.User;
import pl.pollub.myrecommendation.utils.MyUtil;

public class NotificationRecyclerAdapter extends RecyclerView.Adapter<NotificationRecyclerAdapter.ViewHolder> {

    public List<Notification> notificationList;
    private Context mContext;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private OnItemClickListener listener;

    public NotificationRecyclerAdapter(List<Notification> notificationList){
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    // Define listener member variable
    public interface OnItemClickListener{
        void onCardViewClickListener(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }


    @Override
    public void onBindViewHolder(@NonNull final NotificationRecyclerAdapter.ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        holder.bindData(notificationList.get(position));


    }



    @Override
    public int getItemCount() {
        return this.notificationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView ivNotificationProfilePicture;
        private TextView tvNotificationUserName, tvNotificationContent, tvNotificationTimeAgo;
        private View mView;
        private CardView cvNotification;

        public ViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;

            ivNotificationProfilePicture = mView.findViewById(R.id.ivNotificationProfilePicture);
            tvNotificationUserName = mView.findViewById(R.id.tvNotificationUserName);
            tvNotificationContent = mView.findViewById(R.id.tvNotificationContent);
            tvNotificationTimeAgo = mView.findViewById(R.id.tvNotificationTimeAgo);
            cvNotification = mView.findViewById(R.id.cvNotification);

            cvNotification.setOnClickListener(this);

        }

        public void bindData(Notification notification){
            bindUser(notification.getSender());
            bindNotification(notification);
        }

        private void bindNotification(Notification notification) {
            tvNotificationContent.setText(notification.getContent());
            String strTimeAgo = "just now";
            if(notification.getTimestamp() != null){
                strTimeAgo = MyUtil.getTimeAgo(notification.getTimestamp().getTime(), mContext);
            }
            tvNotificationTimeAgo.setText(strTimeAgo);
        }


        private void bindUser(User user){
            tvNotificationUserName.setText(user.getName());
            RequestOptions placeholderOption = new RequestOptions().placeholder(R.color.gray);
            Glide.with(mContext).applyDefaultRequestOptions(placeholderOption)
                    .load(Uri.parse(user.getProfilePicture())).into(ivNotificationProfilePicture);

        }


        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.cvNotification:
                    if(listener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            listener.onCardViewClickListener(itemView, position);
                        }
                    }
                    break;
            }
        }
    }


}
