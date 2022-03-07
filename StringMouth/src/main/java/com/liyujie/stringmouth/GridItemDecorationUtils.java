package com.liyujie.stringmouth;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class GridItemDecorationUtils extends RecyclerView.ItemDecoration{
    private int spacing;

    public GridItemDecorationUtils(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        if(position % 1 == 0){
            outRect.left = spacing / 2;
            outRect.top = spacing / 2;
            outRect.bottom  = spacing / 2;
            outRect.right = spacing / 2;
        }

    }
}