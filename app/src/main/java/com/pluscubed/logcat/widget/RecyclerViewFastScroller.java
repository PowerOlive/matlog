package com.pluscubed.logcat.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.pluscubed.logcat.R;

public class RecyclerViewFastScroller extends FrameLayout {
    private static final String TAG = "RV_FastScroller";

    private static final int TRACK_SNAP_RANGE = 5;
    private ImageView scrollerView;
    private int height;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private boolean registeredObserver = false;

    public RecyclerViewFastScroller(Context context) {
        super(context);
        init();
    }

    public RecyclerViewFastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
    }

    @SuppressLint("ClickableViewAccessibility") @Override public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < (scrollerView.getX() - scrollerView.getPaddingStart())) return false;
                scrollerView.setSelected(true);
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                setScrollerHeight(y);
                setRecyclerViewPosition(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                scrollerView.setSelected(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override protected void onDetachedFromWindow() {
        if (recyclerView != null) {
            recyclerView.removeOnScrollListener(onScrollListener);
            safelyUnregisterObserver();
        }
        super.onDetachedFromWindow();
    }

    private void safelyUnregisterObserver() {
        try {// rare case
            if (registeredObserver && recyclerView.getAdapter() != null) {
                recyclerView.getAdapter().unregisterAdapterDataObserver(observer);
            }
        } catch (Exception ignored) {}
    }

    protected void init() {
        setVisibility(GONE);
        setClipChildren(false);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.fastscroller, this);
        scrollerView = findViewById(R.id.fast_scroller_handle);
        setVisibility(VISIBLE);
    }

    public void attachRecyclerView(final RecyclerView recyclerView) {
        if (this.recyclerView == null) {
            this.recyclerView = recyclerView;
            this.layoutManager = recyclerView.getLayoutManager();
            this.recyclerView.addOnScrollListener(onScrollListener);
            if (recyclerView.getAdapter() != null && !registeredObserver) {
                recyclerView.getAdapter().registerAdapterDataObserver(observer);
                registeredObserver = true;
            }
            hideShow();
            initScrollHeight();
        }
    }

    private void initScrollHeight() {
        if (recyclerView.computeVerticalScrollOffset() == 0) {
            this.recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override public boolean onPreDraw() {
                    RecyclerViewFastScroller.this.recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    iniHeight();
                    return true;
                }
            });
        } else {
            iniHeight();
        }
    }

    protected void iniHeight() {
        if (scrollerView.isSelected()) return;
        int verticalScrollOffset = RecyclerViewFastScroller.this.recyclerView.computeVerticalScrollOffset();
        int verticalScrollRange = RecyclerViewFastScroller.this.computeVerticalScrollRange();
        float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - height);
        setScrollerHeight(height * proportion);
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;
            if (scrollerView.getY() == 0) {
                proportion = 0f;
            } else if (scrollerView.getY() + scrollerView.getHeight() >= height - TRACK_SNAP_RANGE) {
                proportion = 1f;
            } else {
                proportion = y / (float) height;
            }
            int targetPos = getValueInRange(itemCount - 1, (int) (proportion * (float) itemCount));
            if (layoutManager instanceof StaggeredGridLayoutManager) {
                ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(targetPos, 0);
            } else if (layoutManager instanceof GridLayoutManager) {
                ((GridLayoutManager) layoutManager).scrollToPositionWithOffset(targetPos, 0);
            } else {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(targetPos, 0);
            }
        }
    }

    private static int getValueInRange(int max, int value) {
        return Math.min(Math.max(0, value), max);
    }

    private void setScrollerHeight(float y) {
        int handleHeight = scrollerView.getHeight();
        scrollerView.setY(getValueInRange(height - handleHeight, (int) (y - handleHeight / 2)));
    }

    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (scrollerView.isSelected()) return;
            int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
            int verticalScrollRange = recyclerView.computeVerticalScrollRange();
            float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - height);
            setScrollerHeight(height * proportion);
        }
    };

    private final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            hideShow();
        }

        @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            hideShow();
        }

        @Override public void onChanged() {
            super.onChanged();
            hideShow();
        }
    };

    protected void hideShow() {
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            setVisibility(recyclerView.getAdapter().getItemCount() > 2000 ? VISIBLE : GONE);
        } else {
            //setVisibility(GONE);
        }
    }
}