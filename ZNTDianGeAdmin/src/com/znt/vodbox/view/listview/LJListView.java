package com.znt.vodbox.view.listview;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.znt.vodbox.R;
import com.znt.vodbox.entity.Constant;
import com.znt.vodbox.entity.LocalDataEntity;
import com.znt.vodbox.utils.DateUtils;
import com.znt.vodbox.utils.StringUtils;

/**
 * 改编自XListView
 * 
 * 这个LJListView继承自RelativeLayout，导致一些ListView的属性没有，如果大家项目中需要用到ListView的一些属性，请在本类中加入
 * 
 * 示例如下（参见274-276行）：
 * public void setOnItemClickListener(OnItemClickListener listener){
		mListView.setOnItemClickListener(listener);
   }
 * 
 * 然后调用setOnItemClickListener就可以了
 * 
 * 
 * 
 * @author Administrator
 */

public class LJListView extends RelativeLayout implements OnScrollListener
{
	
	private ListView mListView = null;
	private float mLastY = -1; // save event y
	private Scroller mScroller; // used for scroll back
	private OnScrollListener mScrollListener; // user's scroll listener
	
	// the interface to trigger refresh and load more.
	private IXListViewListener mListViewListener;

	// -- header view
	private LJListViewHeader mHeaderView;
	// header view content, use it to calculate the Header's height. And hide it
	// when disable pull refresh.
	private RelativeLayout mHeaderViewContent;
	private TextView mHeaderTimeView;
	private int mHeaderViewHeight; // header view's height
	private boolean mEnablePullRefresh = true;
	private boolean mPullRefreshing = false; // is refreashing.
	
	// -- footer view
	private LJListViewFooter mFooterView;
	private boolean mEnablePullLoad;
	private boolean mPullLoading;
	private boolean mIsFooterReady = false;

	// total list items, used to detect is at the bottom of listview.
	private int mTotalItemCount;

	// for mScroller, scroll back from header or footer.
	private int mScrollBack;
	private final static int SCROLLBACK_HEADER = 0;
	private final static int SCROLLBACK_FOOTER = 1;

	private final static int SCROLL_DURATION = 400; // scroll back duration
	private final static int PULL_LOAD_MORE_DELTA = 50; // when pull up >= 50px
														// at bottom, trigger
														// load more.
	private final static float OFFSET_RADIO = 1.8f; // support iOS like pull
													// feature.
	
	private boolean mIsAnimation = false; //默认为false
	private Animation animationDown = null; //向上动画
	private Animation animationUp = null; //下拉动画
	private TextView infoHint = null; //弹出的那个textview
	private int count = 0; //表示更新了多少条
	private Context context = null;
	private boolean isHintEable = false;
	public boolean isMeasure = false;
	
	/**
	 * @param context
	 */
	public LJListView(Context context)
	{
		super(context);
		initWithContext(context);
	}

	public LJListView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		initWithContext(context);
	}

	public LJListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	@Override
	/**
	 * 重写该方法，达到使ListView适应ScrollView的效果
	 */
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		/*int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
				MeasureSpec.AT_MOST);*/
		isMeasure = true;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
		isMeasure = false;
		
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
	}
	
	private void initWithContext(Context context) 
	{
		this.context = context;
		initAnimation();
		mScroller = new Scroller(context, new DecelerateInterpolator());
		// XListView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		mListView = (ListView) LayoutInflater.from(context).inflate(R.layout.ljlistview, null);
		OnTouchListener touch = new OnTouchListener() 
		{
			
			@Override
			public boolean onTouch(View v, MotionEvent ev)
			{
				// TODO Auto-generated method stub
				if (mLastY == -1) 
				{
					mLastY = ev.getRawY();
				}

				switch (ev.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					mLastY = ev.getRawY();
					break;
				case MotionEvent.ACTION_MOVE:
					final float deltaY = ev.getRawY() - mLastY;
					mLastY = ev.getRawY();
					if (mListView.getFirstVisiblePosition() == 0
							&& (mHeaderView.getVisiableHeight() > 0 || deltaY > 0))
					{
						// the first item is showing, header has shown or pull down.
						updateHeaderHeight(deltaY / OFFSET_RADIO);
						invokeOnScrolling();
					} 
					else if (mListView.getLastVisiblePosition() == mTotalItemCount - 1
							&& (mFooterView.getBottomMargin() > 0 || deltaY < 0))
					{
						// last item, already pulled up or want to pull up.
						updateFooterHeight(-deltaY / OFFSET_RADIO);
					}
					break;
				default:
					mLastY = -1; // reset
					if (mListView.getFirstVisiblePosition() == 0) 
					{
						// invoke refresh
						if (mEnablePullRefresh
								&& mHeaderView.getVisiableHeight() > mHeaderViewHeight) 
						{
							mPullRefreshing = true;
							mHeaderView.setState(LJListViewHeader.STATE_REFRESHING);
							if (mListViewListener != null) 
							{
								mListViewListener.onRefresh();
							}
						}
						resetHeaderHeight();
					}
					if (mListView.getLastVisiblePosition() == mTotalItemCount - 1) 
					{
						// invoke load more.
						if (mEnablePullLoad
								&& mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA) 
						{
							startLoadMore();
						}
						resetFooterHeight();
					}
					break;
				}
				return false;
			}
		};
		mListView.setOnTouchListener(touch);
		mListView.setOnScrollListener(this);
		addView(mListView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		// init header view
		mHeaderView = new LJListViewHeader(context);
		mHeaderViewContent = (RelativeLayout) mHeaderView
				.findViewById(R.id.xlistview_header_content);
		mHeaderTimeView = (TextView) mHeaderView
				.findViewById(R.id.xlistview_header_time);
		mListView.addHeaderView(mHeaderView);
		mHeaderView.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View arg0)
			{
				// TODO Auto-generated method stub
				Log.e("", "");
			}
		});
		// init footer view
		mFooterView = new LJListViewFooter(context);

		// init header height
		mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() 
				{
					@Override
					public void onGlobalLayout() 
					{
						mHeaderViewHeight = mHeaderViewContent.getHeight();
						getViewTreeObserver()
								.removeGlobalOnLayoutListener(this);
					}
				});
		
		infoHint = (TextView) LayoutInflater.from(context).inflate(R.layout.ljlistview_infohint, null);
		LayoutParams para = new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
		para.setMargins(5, 0, 5, 0);
		infoHint.setLayoutParams(para);
		infoHint.setVisibility(View.GONE);
		addView(infoHint);
	}

	public void showHintView()
	{
		isHintEable = true;
	}
	
	private void initAnimation()
	{
		// TODO Auto-generated method stub
		animationUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0.1f, Animation.RELATIVE_TO_SELF, -1);
		animationUp.setDuration(1000);
		animationUp.setRepeatCount(0);
		animationUp.setAnimationListener(new AnimationListener() 
		{
			
			@Override
			public void onAnimationStart(Animation animation) 
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) 
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				// TODO Auto-generated method stub
				infoHint.setVisibility(View.GONE);
			}
		});
		
		animationDown = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0.1f);
		animationDown.setDuration(1000);
		animationDown.setRepeatCount(0);
		animationDown.setFillAfter(true);
		animationDown.setAnimationListener(new AnimationListener() 
		{
			
			@Override
			public void onAnimationStart(Animation animation) 
			{
				// TODO Auto-generated method stub
				if(isHintEable)
					infoHint.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) 
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) 
			{
				// TODO Auto-generated method stub
				new Handler().postDelayed(new Runnable() 
				{
					
					@Override
					public void run()
					{
						if(isHintEable)
							infoHint.startAnimation(animationUp);
					}
				}, 1000);
			}
		});
		
	}
	
	public void showFootView(boolean show)
	{
		if(show)
			mFooterView.setVisibility(View.VISIBLE);
		else
			mFooterView.setVisibility(View.INVISIBLE);
	}

	public void setAdapter(ListAdapter adapter) 
	{
		// make sure XListViewFooter is the last footer view, and only add once.
		if (mIsFooterReady == false) 
		{
			mIsFooterReady = true;
			mListView.addFooterView(mFooterView);
		}
		mListView.setAdapter(adapter);

	}
	
	public void setOnItemClickListener(OnItemClickListener listener)
	{
		mListView.setOnItemClickListener(listener);
	}
	
	public void addHeader(View headerHeader)
	{
		mListView.addHeaderView(headerHeader);
	}
	
	
	public ListView getListView()
	{
		return mListView;
	}
	
	/**
	 * enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	public void setPullRefreshEnable(boolean enable) 
	{
		mEnablePullRefresh = enable;
		if (!mEnablePullRefresh) 
		{ 
			// disable, hide the content
			mHeaderViewContent.setVisibility(View.INVISIBLE);
		} 
		else 
		{
			mHeaderViewContent.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * enable or disable pull up load more feature.
	 * 
	 * @param enable
	 */
	public void setPullLoadEnable(boolean enable,String str) 
	{
		mEnablePullLoad = enable;
		if (!mEnablePullLoad) 
		{
			mFooterView.hide(str);
			mFooterView.setOnClickListener(null);
		}
		else
		{
			mPullLoading = false;
			mFooterView.show();
			mFooterView.setState(LJListViewFooter.STATE_NORMAL);
			// both "pull up" and "click" will invoke load more.
			mFooterView.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					startLoadMore();
				}
			});
		}
	}

	/**
	 * stop refresh, reset header view.
	 */
	public void stopRefresh() 
	{
		if (mPullRefreshing == true) 
		{
			mPullRefreshing = false;
			if(mIsAnimation && isHintEable)
			{
				if(count > 0)
				{
					infoHint.setText(String.format("更新了%s条数据", count));
					infoHint.startAnimation(animationDown);
				}
			}
			resetHeaderHeight();
		}
	}

	/**
	 * stop load more, reset footer view.
	 */
	public void stopLoadMore()
	{
		if (mPullLoading == true) 
		{
			mPullLoading = false;
		}
		if(count < Constant.ONE_PAGE_SIZE)//当前更新的数据小于或等于一页
			mFooterView.setState(LJListViewFooter.STATE_FINISH);
		else
			mFooterView.setState(LJListViewFooter.STATE_NORMAL);
	}
	
	/**
	 * set last refresh time
	 * 
	 * @param time
	 */
	public void setRefreshTime() 
	{
		long lastTime = LocalDataEntity.newInstance(context).getRefreshTime();
		if(lastTime > 0)
			mHeaderTimeView.setText(DateUtils.getStringTime(lastTime));
		else
			mHeaderTimeView.setText(DateUtils.getStringTime(System.currentTimeMillis()));
		
		LocalDataEntity.newInstance(context).setRefreshTime(System.currentTimeMillis());
	}

	private void invokeOnScrolling() 
	{
		if (mScrollListener instanceof OnXScrollListener) 
		{
			OnXScrollListener l = (OnXScrollListener) mScrollListener;
			l.onXScrolling(this);
		}
	}

	private void updateHeaderHeight(float delta) 
	{
		mHeaderView.setVisiableHeight((int) delta
				+ mHeaderView.getVisiableHeight());
		if (mEnablePullRefresh && !mPullRefreshing) 
		{ 
			// 未处于刷新状态，更新箭头
			if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) 
			{
				mHeaderView.setState(LJListViewHeader.STATE_READY);
			} 
			else
			{
				mHeaderView.setState(LJListViewHeader.STATE_NORMAL);
			}
		}
		mListView.setSelection(0); // scroll to top each time
	}

	/**
	 * reset header view's height.
	 */
	private void resetHeaderHeight() 
	{
		int height = mHeaderView.getVisiableHeight();
		/*if (height == 0) // not visible.
		{
			return;
		}*/
		
		// refreshing and header isn't shown fully. do nothing.
		if (mPullRefreshing && height <= mHeaderViewHeight) 
		{
			return;
		}
		int finalHeight = 0; // default: scroll back to dismiss header.
		// is refreshing, just scroll back to show all the header.
		if (mPullRefreshing && height > mHeaderViewHeight)
		{
			finalHeight = mHeaderViewHeight;
		}
		mScrollBack = SCROLLBACK_HEADER;
		mScroller.startScroll(0, height, 0, finalHeight - height,
				SCROLL_DURATION);
		// trigger computeScroll
		invalidate();
	}

	private void updateFooterHeight(float delta) 
	{
		int height = mFooterView.getBottomMargin() + (int) delta;
		if (mEnablePullLoad && !mPullLoading) 
		{
			if (height > PULL_LOAD_MORE_DELTA) 
			{ // height enough to invoke load
													// more.
				mFooterView.setState(LJListViewFooter.STATE_READY);
			} 
			else 
			{
				if(count >= Constant.ONE_PAGE_SIZE)
					mFooterView.setState(LJListViewFooter.STATE_NORMAL);
				else
					mFooterView.setState(LJListViewFooter.STATE_FINISH);
			}
		}
		mFooterView.setBottomMargin(height);

		// setSelection(mTotalItemCount - 1); // scroll to bottom
	}

	private void resetFooterHeight()
	{
		int bottomMargin = mFooterView.getBottomMargin();
		if (bottomMargin > 0) 
		{
			mScrollBack = SCROLLBACK_FOOTER;
			mScroller.startScroll(0, bottomMargin, 0, -bottomMargin,
					SCROLL_DURATION);
			invalidate();
		}
	}

	private void startLoadMore() 
	{
		mPullLoading = true;
		mFooterView.setState(LJListViewFooter.STATE_LOADING);
		if (mListViewListener != null) 
		{
			mListViewListener.onLoadMore();
		}
	}

	@Override
	public void computeScroll() 
	{
		if (mScroller.computeScrollOffset()) 
		{
			if (mScrollBack == SCROLLBACK_HEADER) 
			{
				mHeaderView.setVisiableHeight(mScroller.getCurrY());
			} 
			else 
			{
				mFooterView.setBottomMargin(mScroller.getCurrY());
			}
			postInvalidate();
			invokeOnScrolling();
		}
		super.computeScroll();
	}

	public void setOnScrollListener(OnScrollListener l) 
	{
		mScrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{
		if (mScrollListener != null)
		{
			mScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) 
	{
		// send to user's listener
		mTotalItemCount = totalItemCount;
		if (mScrollListener != null) 
		{
			mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
					totalItemCount);
		}
	}

	public void setXListViewListener(IXListViewListener l)
	{
		mListViewListener = l;
	}

	/**
	 * you can listen ListView.OnScrollListener or this one. it will invoke
	 * onXScrolling when header/footer scroll back.
	 */
	public interface OnXScrollListener extends OnScrollListener 
	{
		public void onXScrolling(View view);
	}

	/**
	 * implements this interface to get refresh/load more event.
	 */
	public interface IXListViewListener 
	{
		public void onRefresh();
		public void onLoadMore();
	}
	
	public void setCount(int count)
	{
		this.count = count;
	}
	
	public void onFresh()
	{
		
		mFooterView.setState(LJListViewFooter.STATE_FINISH);
		
		mPullRefreshing = true;
		mHeaderView.setState(LJListViewHeader.STATE_REFRESHING);
		if (mListViewListener != null) 
		{
			mListViewListener.onRefresh();
		}
		resetHeaderHeight();
		int finalHeight = StringUtils.dip2px(context, 60);  //XListViewHeader中的高度

		mScrollBack = SCROLLBACK_HEADER;
		mScroller.startScroll(0, 0, 0, finalHeight,
				SCROLL_DURATION);
		invalidate();
	}
	
	public void setIsAnimation(boolean isAnimation)
	{
		mIsAnimation = isAnimation;
	}
}
