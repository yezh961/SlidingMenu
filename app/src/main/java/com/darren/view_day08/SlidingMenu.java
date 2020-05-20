package com.darren.view_day08;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;

/**
 * 实现一个qq6.0的侧边栏滚动效果。
 * 步骤：
 * 1、继承自定义HorizontalScrollView，写好两个布局（menu、content），运行看看效果
 * 2、运行后，布局混乱menu、content宽度不对。应对方法:指定内容（屏幕的宽度）和菜单的宽度（屏幕的宽度 - 一段距离(自定义属性)）
 * 修改onFinishInflate()方法指定宽高后，布局正常。
 * 3、初始化刚进入侧边栏是，菜单项是关闭的。
 * 4、处理触摸事件，手指抬起，要么关闭/要么打开菜单栏
 * 5、处理右边内容页的缩放。以及透明度。
 * 6、最后一个效果。退出永远都在内容页的左边（平移）
 * 7、菜单栏打开时，内容页部分设置阴影。
 */
public class SlidingMenu extends HorizontalScrollView {
    //菜单的宽度
    private int mMenuWidth;
    private View mContentView;
    //内容页的阴影view
    private View mShadowView;
    private View mMenuView;
    private GestureDetector mGestureDetector;
    private boolean mMenuIsOpen = false;
    private boolean mIsIterceptor = false;

    public SlidingMenu(Context context) {
        this(context, null);
    }

    public SlidingMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 初始化自定义属性
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SlidingMenu);

        float rightMargin = array.getDimension(
                R.styleable.SlidingMenu_menuRightMargin, ScreenUtils.dip2px(context, 50));
        // 菜单页的宽度是 = 屏幕的宽度 - 右边的一小部分距离（自定义属性）
        mMenuWidth = (int) (getScreenWidth(context) - rightMargin);
        array.recycle();
        GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //只关注快速滑动，只要快速滑动就会回调。
                //条件，菜单页打开时，往左边快速滑动就关闭；
                //菜单页关闭时，往右边快速滑动就打开。
                if (mMenuIsOpen) {
                    //打开
                    if (velocityX < 0) {
                        closeMenu();
                        return true;
                    }
                } else {
                    //关闭
                    if (velocityX > 0) {
                        openMenu();
                        return true;
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        };
        mGestureDetector = new GestureDetector(context, mGestureListener);
    }


    @Override
    /**
     * xml 布局解析完毕后调用
     * //宽度不对，指定宽高。
     */
    protected void onFinishInflate() {
        super.onFinishInflate();
        //获取子view --->LinearLayout
        ViewGroup container = (ViewGroup) getChildAt(0);

        int childCount = container.getChildCount();
        if (childCount != 2) {
            throw new RuntimeException("只能放置两个布局");
        }

        //第一个孩子是菜单页
        mMenuView = container.getChildAt(0);
        //菜单页的宽度是：屏幕宽度 - 一小段 固定值
        ViewGroup.LayoutParams menuParams = mMenuView.getLayoutParams();
        menuParams.width = mMenuWidth;
        mMenuView.setLayoutParams(menuParams);

        //第二个孩子是内容页
        mContentView = container.getChildAt(1);
        ViewGroup.LayoutParams contentParams = mContentView.getLayoutParams();

        //将内容页部分提取出来
        container.removeView(mContentView);
        //在最外面包裹一层阴影
        RelativeLayout contentContainer = new RelativeLayout(getContext());
        contentContainer.addView(mContentView);
        mShadowView = new View(getContext());
        mShadowView.setBackgroundColor(Color.parseColor("#55000000"));
        contentContainer.addView(mShadowView);

        container.addView(contentContainer);
        //内容页指定宽高:屏幕宽度。
        contentParams.width = getScreenWidth(getContext());
        contentContainer.setLayoutParams(contentParams);
        //最后再把容器放回原来的位置
        container.addView(contentContainer);

        //初始化菜单栏进来是关闭的,发现是没用的
//        scrollTo(mMenuWidth,0);

    }

    //布局摆放
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //初始化菜单栏进来是关闭的
        scrollTo(mMenuWidth, 0);
        mShadowView.setAlpha(0.0f);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mIsIterceptor = false;
        //当菜单打开时，点击内容页 需要关闭菜单，还需要拦截事件 且不响应内容页的点击事件
        if (mMenuIsOpen) {
            float currentX = ev.getX();
            if (currentX > mMenuWidth) {
                //关闭菜单，拦截事件
                closeMenu();
                //子View不响应任何点击事件
                mIsIterceptor = true;
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //如果有拦截，就不要执行自己的onTouch
        if (mIsIterceptor) {
            return true;
        }

        //快速滑动如果触发了，up事件就不用触发了
        if (mGestureDetector.onTouchEvent(ev)) {
            return true;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                //处理手指抬起事件
                //根据滚动的距离判断
                int currentScrollX = getScrollX();
                if (currentScrollX > mMenuWidth / 2) {
                    //关闭
                    closeMenu();
                } else {
                    //打开
                    openMenu();
                }
                return true;
            default:
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 处理右边的缩放，左边的缩放和透明度，需要不断的获取当前滚动的位置
     */
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // 变化是 mMenuWidth - 0,因为初始化的时候scrollTo(mMenuWidth,0);
        Log.e("TAG", "l -> " + l);
        // 算一个梯度值
        float scale = 1f * l / mMenuWidth;// scale 变化是 1 - 0
        // 右边的缩放: 最小是 0.7f, 最大是 1f
        float rightScale = 0.7f + 0.3f * scale;
        // 设置右边的缩放,默认是以中心点缩放
        // 设置缩放的中心点位置
        ViewCompat.setPivotX(mContentView, 0);
        ViewCompat.setPivotY(mContentView, mContentView.getMeasuredHeight() / 2);
        ViewCompat.setScaleX(mContentView, rightScale);
        ViewCompat.setScaleY(mContentView, rightScale);

        //菜单的透明度
        //透明度是由半透明--->全透明  范围0.7f - 1.0f
        //缩放范围是0.7f-1.0f
        float alpha = 0.3f + (1 - scale) * 0.7f;
        ViewCompat.setAlpha(mMenuView, alpha);

        //平移1*0.7f
        ViewCompat.setTranslationX(mMenuView, 0.25f * 1);

        //阴影 0-1
        float alphaScale = 1 - scale;
        mShadowView.setAlpha(alphaScale);

    }

    private void closeMenu() {
        //smoothScrollTo 是有动画的。而ScrollTo是无动画的
        smoothScrollTo(mMenuWidth, 0);
        mMenuIsOpen = false;
    }

    private void openMenu() {
        smoothScrollTo(0, 0);
        mMenuIsOpen = true;
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    private int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }
}
