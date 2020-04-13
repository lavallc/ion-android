package io.lava.ion.moods;

import io.lava.ion.R;
import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.widgets.VerticalFragmentStatePagerAdapter;
import io.lava.ion.widgets.VerticalViewPager;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MoodsFragment extends android.app.Fragment implements ILampManagerListener {
	private FragmentManager fm;
	private ArrayList<MoodFromJSON> moods;
	private VerticalViewPager pager;
	private ImageView upArrow, downArrow, leftArrow, rightArrow;
	private LeftRightTapNotifier leftRightArrowNotifier;

	public void setFragmentManager(FragmentManager fragm) {
		this.fm = fragm;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		leftRightArrowNotifier = new LeftRightTapNotifier();
		
		moods = MoodManager.getInstance(getActivity()).getMoods();
		
		View createdView = inflater.inflate(R.layout.fragment_patterns, container, false);

		// setup the vertical view pager that holds all the mood fragments
		pager = (VerticalViewPager) createdView.findViewById(R.id.viewPager);
		pager.setOverScrollMode(View.OVER_SCROLL_NEVER);
		pager.setAdapter(new PatternAdapter(fm));
		
		// listen for view pager changes and set moods / update chevrons
		pager.setOnPageChangeListener(new VerticalViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int index) {
				if (index == 0) {
					// hide up arrow
					upArrow.setVisibility(View.GONE);
				} else if (index == moods.size()-1) {
					// hide down arrow
					downArrow.setVisibility(View.GONE);
				} else {
					// show both arrows
					upArrow.setVisibility(View.VISIBLE);
					downArrow.setVisibility(View.VISIBLE);
				}
				
				// hide right arrow if there are no configs
				if (moods.get(index).getConfigs().size() == 0)
					rightArrow.setVisibility(View.GONE);
				else
					rightArrow.setVisibility(View.VISIBLE);
				
				if (LampManager.getInstanceIfReady() != null) {
					for (Lamp l: LampManager.getInstanceIfReady().getReadyLamps()) {
						l.setMood((byte)moods.get(index).getId(), null);
					}
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
		
		// get a reference to all the chevrons we need to manipulate
		upArrow = (ImageView)createdView.findViewById(R.id.upArrow);
		downArrow = (ImageView)createdView.findViewById(R.id.downArrow);
		rightArrow = (ImageView)createdView.findViewById(R.id.rightArrow);
		leftArrow = (ImageView)createdView.findViewById(R.id.leftArrow);
		
		upArrow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (pager.getCurrentItem() > 0) {
					pager.setCurrentItem(pager.getCurrentItem()-1, true);
				}
			}
		});
		
		downArrow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (pager.getCurrentItem() < pager.getAdapter().getCount()-1) {
					pager.setCurrentItem(pager.getCurrentItem()+1, true);
				}
			}
		});
		
		leftArrow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				leftRightArrowNotifier.onLeftTap();
			}
		});
		
		rightArrow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				leftRightArrowNotifier.onRightTap();
			}
		});
		
		sendPagerToPosition();
		
		return createdView;
	}

	public static android.app.Fragment newInstance(FragmentManager fragm) {
		MoodsFragment f = new MoodsFragment();
		f.setFragmentManager(fragm);
		return f;
	}
	
	private void sendPagerToPosition() {
		// set the pager to the current connected lamp's mood (otherwise set to first item)
		if (LampManager.getInstanceIfReady().isSingleLampSelected()) {
			int index = 0;
			
			for (int moodId : MoodManager.getInstance(getActivity()).getAllMoodIds()) {
				if (moodId == LampManager.getInstanceIfReady().getSelectedLampIfExists().getCurrentMood()) {
					pager.setCurrentItem(index);
					
					if (index == 0) {
						upArrow.setVisibility(View.GONE);
						downArrow.setVisibility(View.VISIBLE);
					} else if (index == moods.size()-1) {
						downArrow.setVisibility(View.GONE);
						upArrow.setVisibility(View.VISIBLE);
					} else {
						downArrow.setVisibility(View.VISIBLE);
						upArrow.setVisibility(View.VISIBLE);
					}
					
					if (moods.get(index).getConfigs().size() == 0)
						rightArrow.setVisibility(View.GONE);
					else
						rightArrow.setVisibility(View.VISIBLE);
					
					break;
				}
				index++;
			}
			
			// hack to trick the fragment into thinking that the mood changed so it loads in the current configs
			LampManager.getInstanceIfReady().lampMoodDidChange(LampManager.getInstanceIfReady().getSelectedLampIfExists().getCurrentMood());
		} else {
			pager.setCurrentItem(0);
			upArrow.setVisibility(View.GONE);
			downArrow.setVisibility(View.VISIBLE);
			
			if (moods.get(0).getConfigs().size() == 0)
				rightArrow.setVisibility(View.GONE);
			else
				rightArrow.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onPause() {
		LampManager.getInstanceIfReady().removeListener(this);
		
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		sendPagerToPosition();
		
		LampManager.getInstanceIfReady().addListener(this);
	}

	private class PatternAdapter extends VerticalFragmentStatePagerAdapter {
		public PatternAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int pos) {
			return VerticalPatternFragment.newInstance(moods.get(pos), MoodsFragment.this, pager, pos == 0, pos == moods.size()-1, pos, leftRightArrowNotifier);
		}

		@Override
		public int getCount() {
			return moods.size();
		}
	}

	public void setMoodConfig(MoodFromJSON mood, MoodConfigFromJSON config, MoodScroller scroller) {
		// make sure the config panel is actually open (prevents weird changes when the fragment first loads in)
		if (scroller.isRight()) {
			if (LampManager.getInstanceIfReady() != null) {
				for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
					l.setMoodConfig((byte)mood.getId(), (byte)config.getId(), (int)config.getCurrent());
				}
			}
		}
	}
	
	public void setMoodConfigDual(MoodFromJSON mood, MoodConfigFromJSON configOne, MoodConfigFromJSON configTwo, MoodScroller scroller) {
		// make sure the config panel is actually open (prevents weird changes when the fragment first loads in)
		if (scroller.isRight()) {
			if (LampManager.getInstanceIfReady() != null) {
				for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
					l.setMoodConfigDual((byte)mood.getId(), (byte)configOne.getId(), (int)configOne.getCurrent(), (byte)configTwo.getId(), (int)configTwo.getCurrent());
				}
			}
		}
	}

	@Override
	public void onLampListUpdate() {

	}

	@Override
	public void singleLampSelected(Lamp lamp) {
		sendPagerToPosition();
	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {

	}

	@Override
	public void noLampsSelected() {

	}

	@Override
	public void onMoodChange(int moodId) {

	}

	@Override
	public void onWeatherLocationUpdate() {

	}

	@Override
	public void onDfuLampListUpdate() {

	}
}