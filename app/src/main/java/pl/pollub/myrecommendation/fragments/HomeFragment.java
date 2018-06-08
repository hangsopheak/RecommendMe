package pl.pollub.myrecommendation.fragments;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.pollub.myrecommendation.CommentActivity;
import pl.pollub.myrecommendation.DetailRecommendationActivity;
import pl.pollub.myrecommendation.MainActivity;
import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.adapters.RecommendationRecyclerAdapter;
import pl.pollub.myrecommendation.models.Recommendation;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private static final int PAGE_SIZE = 4;

    private RecyclerView recommendationRecyclerView;
    private ArrayList<Recommendation> recommendationList;
    private ArrayList<Recommendation> savedRecommendationList;
    private FirebaseFirestore mFireStore;
    private RecommendationRecyclerAdapter recommendationRecyclerAdapter;
    private ProgressBar progressBar;

    private OnFragmentInteractionListener mListener;

    private DocumentSnapshot lastVisible;
    private DocumentSnapshot lastSavedVisible;
    private boolean isFirstPageFirstLoad;
    private String categoryId;
    private String currentUserId;
    private String profileUserId;

    private MainActivity mainActivity;
    public HomeFragment() { }

    @SuppressLint("ValidFragment")
    public HomeFragment(String profileUserId){
        this.profileUserId = profileUserId;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view =  inflater.inflate(R.layout.fragment_home, container, false);
        mainActivity = (MainActivity) getActivity();
        mainActivity.findViewById(R.id.rvHomeCategory).setVisibility(View.VISIBLE);

        mFireStore = FirebaseFirestore.getInstance();

        categoryId = mainActivity.getSelectedCategoryId();
        currentUserId = mainActivity.getCurrentUserId();

        isFirstPageFirstLoad = true;
        lastSavedVisible = null;
        lastVisible = null;

        recommendationList = new ArrayList<>();
        savedRecommendationList = new ArrayList<>();

        recommendationRecyclerView =  view.findViewById(R.id.rvRecommendation);
        progressBar = view.findViewById(R.id.pbHomeFragment);
        if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED){
            recommendationRecyclerAdapter = new RecommendationRecyclerAdapter(savedRecommendationList, currentUserId);
        }else{
            recommendationRecyclerAdapter = new RecommendationRecyclerAdapter(recommendationList, currentUserId);
        }

        recommendationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recommendationRecyclerView.setAdapter(recommendationRecyclerAdapter);

        recommendationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                if(reachedBottom){
                    if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED) {
                        loadMoreSavedRecommendation();
                    }else{
                        loadMoreRecommendation();
                    }

                }
            }
        });

        if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED){
            loadFirstSavedRecommendation();
        }else{
            loadFirstRecommendation();
        }


        recommendationRecyclerAdapter.setOnItemClickListener(new RecommendationRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onTitleClickListener(View itemView, int position) {
                if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED){
                    toDetail(savedRecommendationList.get(position).getId());
                }else{
                    toDetail(recommendationList.get(position).getId());
                }

            }

            @Override
            public void onImageClickListener(View itemView, int position) {
                if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED){
                    toDetail(savedRecommendationList.get(position).getId());
                }else{
                    toDetail(recommendationList.get(position).getId());
                }

            }

            @Override
            public void onSaveClickListener(View itemView, int position) {
                if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED) {
                    String recommendationId = savedRecommendationList.get(position).getId();
                    String userId = savedRecommendationList.get(position).getUserId();
                    String categoryId = savedRecommendationList.get(position).getCategoryId();
                    checkSaveRecommendation(recommendationId, userId, categoryId);
                }else{
                    String recommendationId = recommendationList.get(position).getId();
                    String userId = recommendationList.get(position).getUserId();
                    String categoryId = recommendationList.get(position).getCategoryId();
                    checkSaveRecommendation(recommendationId, userId, categoryId);
                }

            }

            @Override
            public void onCommentClickListener(View itemView, int position) {
                Intent intent = new Intent(getActivity(), CommentActivity.class);
                String recommendationId = null;
                if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_SAVED){
                    recommendationId = savedRecommendationList.get(position).getId();
                }else{
                    recommendationId = recommendationList.get(position).getId();
                }
                intent.putExtra("recommendation_id", recommendationId);
                startActivity(intent);
            }
        });
        return view;
    }

    private void loadFirstRecommendation(){
        progressBar.setVisibility(View.VISIBLE);
        CollectionReference firstCollectionReference = mFireStore.collection("Recommendation");
        Query firstQuery;

        if(!categoryId.equals("0")){
            firstQuery = firstCollectionReference
                    .whereEqualTo("category_id", categoryId);
            if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_PROFILE){
                firstQuery = firstQuery.whereEqualTo("user_id", profileUserId);
            }
            firstQuery = firstQuery.orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        }else{
            firstQuery = firstCollectionReference;
            if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_PROFILE){
                firstQuery = firstQuery.whereEqualTo("user_id", profileUserId);
            }
            firstQuery = firstQuery
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        }

        firstQuery.addSnapshotListener(getActivity(),new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(isFirstPageFirstLoad && documentSnapshots != null){
                    if(documentSnapshots.size() > 0){
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() -1);
                    }
                }
                if(documentSnapshots == null){
                    progressBar.setVisibility(View.INVISIBLE);
                    return;
                }
                for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if(doc.getType() == DocumentChange.Type.ADDED){
                        final Recommendation recommendation = mapRecommendation(doc);
                        if(isFirstPageFirstLoad){
                            recommendationList.add(recommendation);
                        }else{
                            recommendationList.add(0,recommendation);
                        }
                    }
                }
                recommendationRecyclerAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.INVISIBLE);
                isFirstPageFirstLoad = false;

            }
        });
    }

    private void loadFirstSavedRecommendation(){
        progressBar.setVisibility(View.VISIBLE);
        CollectionReference firstCollectionReference = mFireStore.collection("Users/" + currentUserId + "/saved_recommendation");
        Query firstQuery;

        if(!categoryId.equals("0")){
            firstQuery = firstCollectionReference
                    .whereEqualTo("category_id", categoryId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        }else{
            firstQuery = firstCollectionReference
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        }

        firstQuery.addSnapshotListener(getActivity(),new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(isFirstPageFirstLoad && documentSnapshots != null){
                    if(documentSnapshots.size() > 0){
                        lastSavedVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() -1);
                    }
                }
                if(documentSnapshots == null){
                    progressBar.setVisibility(View.INVISIBLE);
                    return;
                }
                for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if(doc.getType() == DocumentChange.Type.ADDED){
                        String recId = doc.getDocument().getId().toString();
                        mFireStore.collection("Recommendation").document(recId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                                Recommendation recommendation = new Recommendation();
                                recommendation.setId(documentSnapshot.getId());
                                recommendation.setUserId(documentSnapshot.get("user_id").toString());
                                recommendation.setIdea(documentSnapshot.get("idea").toString());
                                recommendation.setImageUri(Uri.parse(documentSnapshot.get("image_url").toString()));
                                recommendation.setRecommendationType(Integer.parseInt(documentSnapshot.get("recommendation_type").toString()));
                                recommendation.setCategoryId(documentSnapshot.get("category_id").toString());
                                recommendation.setTitle(documentSnapshot.get("title").toString());
                                recommendation.setDescription(documentSnapshot.get("description").toString());
                                recommendation.setTimstamp( (Date) documentSnapshot.get("timestamp"));

                                if(isFirstPageFirstLoad){
                                    savedRecommendationList.add(recommendation);
                                }else{
                                    savedRecommendationList.add(0,recommendation);
                                }
                                recommendationRecyclerAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
                progressBar.setVisibility(View.INVISIBLE);
                isFirstPageFirstLoad = false;

            }
        });
    }

    private void loadMoreRecommendation() {
        progressBar.setVisibility(View.VISIBLE);
        CollectionReference nextCollectionReference = mFireStore.collection("Recommendation");
        Query nextQuery = nextCollectionReference;
        if(!categoryId.equals("0")){
            if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_PROFILE){
                nextQuery = nextQuery.whereEqualTo("user_id", profileUserId);
            }
            nextQuery = nextQuery
                    .whereEqualTo("category_id", categoryId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(PAGE_SIZE);
        }else{
            if(mainActivity.getFragmentType() == MainActivity.FRAGMENT_TYPE_PROFILE){
                nextQuery = nextQuery.whereEqualTo("user_id", profileUserId);
            }
            nextQuery = nextQuery
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(PAGE_SIZE);
        }
        nextQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(documentSnapshots != null){
                    if(documentSnapshots.size() > 0)
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() -1);
                    for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                        if(doc.getType() == DocumentChange.Type.ADDED){
                            final Recommendation recommendation = mapRecommendation(doc);
                            recommendationList.add(recommendation);

                        }
                    }
                    recommendationRecyclerAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.INVISIBLE);
                    isFirstPageFirstLoad = false;
                }else{
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void loadMoreSavedRecommendation() {
        progressBar.setVisibility(View.VISIBLE);
        CollectionReference nextCollectionReference = mFireStore.collection("Users/" + currentUserId + "/saved_recommendation");
        Query nextQuery;
        if(!categoryId.equals("0")){
            nextQuery = nextCollectionReference
                    .whereEqualTo("category_id", categoryId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastSavedVisible)
                    .limit(PAGE_SIZE);
        }else{
            nextQuery = nextCollectionReference
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastSavedVisible)
                    .limit(PAGE_SIZE);
        }
        nextQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(documentSnapshots != null){
                    if(documentSnapshots.size() > 0)
                        lastSavedVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() -1);
                    for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                        if(doc.getType() == DocumentChange.Type.ADDED){
                            String recId = doc.getDocument().getId().toString();
                            mFireStore.collection("Recommendation").document(recId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                                    Recommendation recommendation = new Recommendation();
                                    recommendation.setId(documentSnapshot.getId());
                                    recommendation.setUserId(documentSnapshot.get("user_id").toString());
                                    recommendation.setIdea(documentSnapshot.get("idea").toString());
                                    recommendation.setImageUri(Uri.parse(documentSnapshot.get("image_url").toString()));
                                    recommendation.setRecommendationType(Integer.parseInt(documentSnapshot.get("recommendation_type").toString()));
                                    recommendation.setCategoryId(documentSnapshot.get("category_id").toString());
                                    recommendation.setTitle(documentSnapshot.get("title").toString());
                                    recommendation.setDescription(documentSnapshot.get("description").toString());
                                    recommendation.setTimstamp( (Date) documentSnapshot.get("timestamp"));

                                    savedRecommendationList.add(recommendation);
                                    recommendationRecyclerAdapter.notifyDataSetChanged();
                                }
                            });

                        }
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                    isFirstPageFirstLoad = false;
                }else{
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }


    private Recommendation mapRecommendation(DocumentChange doc){
        HashMap<String, Object> changedData = (HashMap<String, Object>) doc.getDocument().getData();
        Recommendation recommendation = new Recommendation();
        recommendation.setId(doc.getDocument().getId().toString());
        recommendation.setUserId(changedData.get("user_id").toString());
        recommendation.setIdea(changedData.get("idea").toString());
        recommendation.setImageUri(Uri.parse(changedData.get("image_url").toString()));
        recommendation.setRecommendationType(Integer.parseInt(changedData.get("recommendation_type").toString()));
        recommendation.setCategoryId(changedData.get("category_id").toString());
        recommendation.setTitle(changedData.get("title").toString());
        recommendation.setDescription(changedData.get("description").toString());
        recommendation.setTimstamp( (Date) changedData.get("timestamp"));
        return recommendation;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void toDetail(String recommendationId){
        Intent detailRecIntent = new Intent(getContext(), DetailRecommendationActivity.class);
        detailRecIntent.putExtra("recommendation_id", recommendationId);
        startActivity(detailRecIntent);
    }

    private void addSaveUserRecommendation(String recommendationId, String userId, String categoryId){

        Map<String, Object> recommendationSavedMap = new HashMap<>();
        recommendationSavedMap.put("user_id", currentUserId);
        recommendationSavedMap.put("timestamp", FieldValue.serverTimestamp());
        mFireStore.collection("Recommendation/" + recommendationId + "/saved_users").document(currentUserId).set(recommendationSavedMap)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    recommendationRecyclerAdapter.notifyDataSetChanged();
                    if(!task.isSuccessful()){
                        String error = task.getException().getMessage();
                        Toast.makeText(getActivity(), "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            }
        );

        Map<String, Object> userSavedMap = new HashMap<>();
        userSavedMap.put("recommendation_id", recommendationId);
        userSavedMap.put("timestamp", FieldValue.serverTimestamp());
        userSavedMap.put("category_id", categoryId);
        mFireStore.collection("Users/" + currentUserId +"/saved_recommendation").document(recommendationId).set(userSavedMap)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful()){
                        String error = task.getException().getMessage();
                        Toast.makeText(getActivity(), "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            }
        ) ;

        saveNotification(recommendationId, userId);
    }

    private void saveNotification(String recommendationId, String userId){
        if(userId.equals(currentUserId)) return;

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("sender_id", currentUserId);
        notificationMap.put("recommendation_id", recommendationId);
        notificationMap.put("timestamp", FieldValue.serverTimestamp());
        notificationMap.put("unseen", true);
        notificationMap.put("type", 1);
        mFireStore.collection("Users/" + userId +"/notification").document().set(notificationMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                           @Override
                                           public void onComplete(@NonNull Task<Void> task) {
                                               if(!task.isSuccessful()){
                                                   String error = task.getException().getMessage();
                                                   Toast.makeText(getActivity(), "FireStore Error: " + error, Toast.LENGTH_LONG).show();
                                               }
                                           }
                                       }
                ) ;
    }

    private void deleteSaveUserRecommendation(final String recommendationId){
        mFireStore.collection("Recommendation/" + recommendationId + "/saved_users" ).document(currentUserId).delete();
        mFireStore.collection("Users/" + currentUserId + "/saved_recommendation").document(recommendationId).delete();

    }

    private void checkSaveRecommendation(final String recommendationId, final String userId, final String categoryId){
        mFireStore.collection("Recommendation/" + recommendationId +"/saved_users").document(currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult().exists()){
                            deleteSaveUserRecommendation(recommendationId);
                        }else{
                            addSaveUserRecommendation(recommendationId, userId, categoryId);
                        }
                    }
        });
    }

}
