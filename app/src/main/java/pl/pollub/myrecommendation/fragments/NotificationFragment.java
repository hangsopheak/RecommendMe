package pl.pollub.myrecommendation.fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import pl.pollub.myrecommendation.CommentActivity;
import pl.pollub.myrecommendation.DetailRecommendationActivity;
import pl.pollub.myrecommendation.MainActivity;
import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.adapters.NotificationRecyclerAdapter;
import pl.pollub.myrecommendation.adapters.RecommendationRecyclerAdapter;
import pl.pollub.myrecommendation.models.Notification;
import pl.pollub.myrecommendation.models.Recommendation;
import pl.pollub.myrecommendation.utils.MyUtil;

import static android.widget.LinearLayout.HORIZONTAL;

/**
 * A simple {@link Fragment} subclass.
 */
public class NotificationFragment extends Fragment {

    private static final int PAGE_SIZE = 10;

    private RecyclerView notificationRecyclerview;
    private NotificationRecyclerAdapter notificationRecyclerAdapter;
    private ArrayList<Notification> notificationList;
    private FirebaseFirestore mFireStore;
    private ProgressBar progressBar;

    private DocumentSnapshot lastVisible;
    private boolean isNotificationFirstLoad;
    private String currentUserId;
    private MainActivity mainActivity;

    public NotificationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view =  inflater.inflate(R.layout.fragment_notification, container, false);
        mainActivity = (MainActivity) getActivity();
        mainActivity.findViewById(R.id.rvHomeCategory).setVisibility(View.GONE);
        currentUserId = mainActivity.getCurrentUserId();

        mFireStore = FirebaseFirestore.getInstance();
        isNotificationFirstLoad = true;
        notificationList = new ArrayList<>();
        notificationRecyclerview = view.findViewById(R.id.rvNotification);
        notificationRecyclerAdapter = new NotificationRecyclerAdapter(notificationList);
        notificationRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        notificationRecyclerview.setAdapter(notificationRecyclerAdapter);

        notificationRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                if(reachedBottom){
                    loadMoreNotification();
                }
            }
        });

        notificationRecyclerAdapter.setOnItemClickListener(new NotificationRecyclerAdapter.OnItemClickListener() {

            @Override
            public void onCardViewClickListener(View itemView, int position) {
                Notification notification = notificationList.get(position);
                if(notification.getType() == Notification.TYPE_SAVE){
                    toDetail(notification.getRecommendationId());
                }else{
                    loadRecommendation(notification.getRecommendationId());
                }

            }

        });
        clearUnseen();

        CollectionReference firstCollectionReference = mFireStore.collection("Users/" + currentUserId + "/notification" );
        Query firstQuery = firstCollectionReference
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        firstQuery.addSnapshotListener(getActivity(),new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(isNotificationFirstLoad && documentSnapshots != null){
                    if(documentSnapshots.size() > 0){
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() -1);
                    }
                }
                for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if(doc.getType() == DocumentChange.Type.ADDED){
                        final Notification notification = mapNotification(doc);
                        if(isNotificationFirstLoad){
                            notificationList.add(notification);
                        }else{
                            notificationList.add(0,notification);
                        }
                    }
                }
                notificationRecyclerAdapter.notifyDataSetChanged();
                isNotificationFirstLoad = false;
            }
        });
        return view;
    }

    private void loadMoreNotification() {
        CollectionReference nextCollectionReference = mFireStore.collection("Users/" + currentUserId + "/notification" );
        Query nextQuery = nextCollectionReference
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        nextQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(documentSnapshots != null){
                    if(documentSnapshots.size() > 0)
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() -1);
                    for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                        if(doc.getType() == DocumentChange.Type.ADDED){
                            final Notification notification = mapNotification(doc);
                            notificationList.add(notification);
                        }
                    }
                    notificationRecyclerAdapter.notifyDataSetChanged();
                    isNotificationFirstLoad = false;
                }else{
                }
            }
        });
    }

    private Notification mapNotification(DocumentChange doc) {
        HashMap<String, Object> changedData = (HashMap<String, Object>) doc.getDocument().getData();
        Notification notification = new Notification();
        notification.setId(doc.getDocument().getId().toString());
        notification.setRecommendationId(changedData.get("recommendation_id").toString());
        notification.setSenderId(changedData.get("sender_id").toString());
        notification.setType(Integer.parseInt(changedData.get("type").toString()));
        notification.setUnseen((boolean) changedData.get("unseen"));
        notification.setTimestamp((Date) changedData.get("timestamp"));
        return notification;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void clearUnseen(){
        mFireStore.collection("Users/" + currentUserId +"/notification")
                .whereEqualTo("unseen", true).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult().getDocuments().size() > 0){
                        for(DocumentSnapshot doc:task.getResult().getDocuments()){
                            mFireStore.collection("Users/" + currentUserId + "/notification")
                                    .document(doc.getId()).update("unseen", false);
                        }
                        mainActivity.setNotification();
                    }
                }
            }
        });
    }

    private void loadRecommendation(final String recommendationId) {
        mFireStore.collection("Recommendation").document(recommendationId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    DocumentSnapshot data = task.getResult();
                    toComment(recommendationId, data.getData().get("user_id").toString());
                }
            }
        });
    }

    private void toDetail(String recommendationId){
        Intent detailRecIntent = new Intent(getContext(), DetailRecommendationActivity.class);
        detailRecIntent.putExtra("recommendation_id", recommendationId);
        startActivity(detailRecIntent);
    }

    private void toComment(String recommendationId, String userId){
        Intent intent = new Intent(getContext(), CommentActivity.class);
        intent.putExtra("recommendation_id", recommendationId);
        intent.putExtra("recommendation_user_id", userId);
        startActivity(intent);
    }

}
