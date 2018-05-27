package pl.pollub.myrecommendation.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.pollub.myrecommendation.R;
import pl.pollub.myrecommendation.models.Category;

public class HomeCategoryRecyclerAdapter extends RecyclerView.Adapter<HomeCategoryRecyclerAdapter.ViewHolder> {

    public List<Category> categoryList;
    private Context mContext;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private HomeCategoryRecyclerAdapter.OnItemClickListener listener;
    private String selectedCategoryId = "0";

    public HomeCategoryRecyclerAdapter(List<Category> categoryList){
        this.categoryList = categoryList;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    @NonNull
    @Override
    public HomeCategoryRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.home_category_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final HomeCategoryRecyclerAdapter.ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        String id = categoryList.get(position).getId();
        String name = categoryList.get(position).getName();
        String icon = categoryList.get(position).getIcon();

        holder.setName(name);
        holder.setIcon(icon);
        holder.setSelected(id);

        mFireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


    }

    // Define listener member variable
    public interface OnItemClickListener{
        //void onItemClick(View itemView, int position);
        void onClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(HomeCategoryRecyclerAdapter.OnItemClickListener listener){
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return this.categoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName;
        private ImageView ivIcon;
        private ConstraintLayout constraintLayout;
        private View mView;

        public ViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;
            ivIcon = mView.findViewById(R.id.ivHomeCategoryIcon);
            tvName = mView.findViewById(R.id.tvHomeCategoryName);
            constraintLayout = mView.findViewById(R.id.homeCategoryLayout);

            ivIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    triggerClick();
                }
            });

            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    triggerClick();
                }
            });

        }

        private void triggerClick(){
            if(listener != null){
                int position = getAdapterPosition();
                if(position != RecyclerView.NO_POSITION){
                    listener.onClick(itemView, position);
                }
            }
        }

        public void setName(String name){
            tvName.setText(name);
        }

        public void setIcon(String iconURI){
            if(iconURI == null){
                ivIcon.setVisibility(View.GONE);
                return;
            }
            RequestOptions placeholderOption = new RequestOptions().placeholder(R.drawable.ic_launcher_background);
            Glide.with(mContext).applyDefaultRequestOptions(placeholderOption)
                    .load(iconURI).into(ivIcon);
        }

        public void setSelected(String itemId){
            Drawable layoutBorder = (Drawable) mContext.getDrawable(R.drawable.layout_border);
            Drawable selectedLayoutBorder = (Drawable) mContext.getDrawable(R.drawable.layout_border_selected);
            if(itemId.equals(selectedCategoryId)){
                constraintLayout.setBackground(selectedLayoutBorder);
            }else{
                constraintLayout.setBackground(layoutBorder);
            }

        }
    }


}
