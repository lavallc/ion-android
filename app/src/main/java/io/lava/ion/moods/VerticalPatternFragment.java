package io.lava.ion.moods;

import io.lava.ion.R;
import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.connectivity.lamp.MoodConfig;
import io.lava.ion.connectivity.lamp.MoodConfigCallback;
import io.lava.ion.connectivity.lamp.SuccessFailCallback;
import io.lava.ion.utility.ImageLoaderTask;
import io.lava.ion.utility.SlidingImageView;
import io.lava.ion.widgets.VerticalViewPager;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.buzzingandroid.ui.HSVColorWheel;
import com.buzzingandroid.ui.OnColorSelectedListener;
import com.ikovac.timepickerwithseconds.view.TimePicker;
import com.ikovac.timepickerwithseconds.view.TimePickerDialog;

public class VerticalPatternFragment extends Fragment implements ILampManagerListener {
	private MoodFromJSON moodInfo;
	private MoodsFragment moodsFragment;
	private VerticalViewPager verticalPager;
	private boolean first, last;
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, View> moodConfigViews = new HashMap<Integer, View>();
	private ImageView favoriteBtn;
	private Button restoreBtn;
	private boolean fragmentVisible = false;
	private MoodScroller patScroller;
	private int pageIndex;
	private LeftRightTapNotifier leftRightArrowNotifier;
	
	public static VerticalPatternFragment newInstance(MoodFromJSON moodInfo, MoodsFragment moodsFragment, VerticalViewPager verticalPager, boolean first, boolean last, int pageIndex, LeftRightTapNotifier leftRightArrowNotifier) {
    	VerticalPatternFragment f = new VerticalPatternFragment();
      
    	f.moodInfo = moodInfo;
        f.moodsFragment = moodsFragment;
        f.verticalPager = verticalPager;
        f.first = first;
        f.last = last;
        f.pageIndex = pageIndex;
        f.leftRightArrowNotifier = leftRightArrowNotifier;
        
        return f;
    }
	
	private void setDefaultHueSat(MoodConfigFromJSON hueConfig, MoodConfigFromJSON satConfig, HSVColorWheel colorWheel) {
		// get default hue
		float hue = (float)hueConfig.getDefault();
		// load up saturation from NEXT config (which is always saturation)
		float sat = ((float)satConfig.getDefault())/255f;
		
		// set default color on wheel
		float[] defColor = {hue, sat, 1.0f};
		colorWheel.setColor(Color.HSVToColor(defColor));
	}
	
	private void setStateOfConfigsAndRotationFavoriteBtn() {
		if (LampManager.getInstanceIfReady().areMultipleLampsSelected() || LampManager.getInstanceIfReady().areNoLampsSelected()) {
			// hide favorite btn
			favoriteBtn.setVisibility(View.GONE);
			
			// set all configs to default
			for (int i=0; i<moodInfo.getConfigs().size(); i++) {
				// these are the only widgets that actually have 'state'
				if (moodInfo.getConfigs().get(i).getWidget().equals("color")) {
					HSVColorWheel colorWheel = (HSVColorWheel) moodConfigViews.get(moodInfo.getConfigs().get(i).getId());
					
					// set default color on wheel
					setDefaultHueSat(moodInfo.getConfigs().get(i), moodInfo.getConfigs().get(i+1), colorWheel);
				} else if (moodInfo.getConfigs().get(i).getWidget().equals("slider")) {
					SeekBar slider = (SeekBar) moodConfigViews.get(moodInfo.getConfigs().get(i).getId());
					
					slider.setProgress((int) moodInfo.getConfigs().get(i).getDefault());
				} else if (moodInfo.getConfigs().get(i).getWidget().equals("toggle")) {
					Switch toggle = (Switch) moodConfigViews.get(moodInfo.getConfigs().get(i).getId());
					
					toggle.setChecked(moodInfo.getConfigs().get(i).getDefault() != 0);
				}
			}
		} else if (LampManager.getInstanceIfReady().isSingleLampSelected()) {
			// only show the favorite button if the mood is NOT OFF
			if (!moodInfo.getName().equalsIgnoreCase("off")) {
				// set state of favorite btn
				if (LampManager.getInstanceIfReady().getSelectedLampIfExists().getRotation().contains(moodInfo.getId())) {
					// this mood is in the lamp's rotation
					favoriteBtn.setImageResource(R.drawable.favorited_on);
				} else {
					// this mood is not in the lamp's rotation
					favoriteBtn.setImageResource(R.drawable.favorited_off);
				}
				favoriteBtn.setVisibility(View.VISIBLE);
			}
			
			// get state for all mood configs
			for (int i=0; i<moodInfo.getConfigs().size(); i++) {
				final int index = i;
				
				// these are the only widgets that actually have 'state'
				if (moodInfo.getConfigs().get(index).getWidget().equals("color")) {
					final HSVColorWheel colorWheel = (HSVColorWheel) moodConfigViews.get(moodInfo.getConfigs().get(index).getId());
					
					LampManager.getInstanceIfReady().getSelectedLampIfExists().getMoodConfig(moodInfo.getId(), moodInfo.getConfigs().get(index).getId(), new MoodConfigCallback() {
						@Override
						public void onSuccess(final MoodConfig hueMoodConfig) {
							LampManager.getInstanceIfReady().getSelectedLampIfExists().getMoodConfig(moodInfo.getId(), moodInfo.getConfigs().get(index+1).getId(), new MoodConfigCallback() {
								@Override
								public void onSuccess(final MoodConfig satMoodConfig) {
									float hue = (float)hueMoodConfig.getConfigVal();
									float sat = ((float)satMoodConfig.getConfigVal())/255f;
									
									float[] defColor = {hue, sat, 1.0f};
									colorWheel.setColor(Color.HSVToColor(defColor));
								}

								@Override
								public void onFail() {
									// set default color on wheel
									setDefaultHueSat(moodInfo.getConfigs().get(index), moodInfo.getConfigs().get(index+1), colorWheel);
								}
							});
						}

						@Override
						public void onFail() {
							// set default color on wheel
							setDefaultHueSat(moodInfo.getConfigs().get(index), moodInfo.getConfigs().get(index+1), colorWheel);
						}
					});
				} else if (moodInfo.getConfigs().get(index).getWidget().equals("slider")) {
					final SeekBar slider = (SeekBar) moodConfigViews.get(moodInfo.getConfigs().get(index).getId());
					
					LampManager.getInstanceIfReady().getSelectedLampIfExists().getMoodConfig(moodInfo.getId(), moodInfo.getConfigs().get(index).getId(), new MoodConfigCallback() {
						@Override
						public void onSuccess(MoodConfig moodConfig) {
							slider.setProgress((int) moodConfig.getConfigVal());
						}

						@Override
						public void onFail() {
							slider.setProgress((int) moodInfo.getConfigs().get(index).getDefault());
						}
					});
				} else if (moodInfo.getConfigs().get(index).getWidget().equals("toggle")) {
					final Switch toggle = (Switch) moodConfigViews.get(moodInfo.getConfigs().get(index).getId());
					
					LampManager.getInstanceIfReady().getSelectedLampIfExists().getMoodConfig(moodInfo.getId(), moodInfo.getConfigs().get(index).getId(), new MoodConfigCallback() {
						@Override
						public void onSuccess(MoodConfig moodConfig) {
							toggle.setChecked(moodConfig.getConfigVal() != 0);
						}

						@Override
						public void onFail() {
							toggle.setChecked(moodInfo.getConfigs().get(index).getDefault() != 0);
						}
					});
				}
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		fragmentVisible = true;
		
		patScroller.setLeftRightArrowNotifier(leftRightArrowNotifier);
		
		// ensure the config scroller is showing the left side of the scrollview
		patScroller.sendToLeft();
		
		// set the state of configs and favorite btn
		//setStateOfConfigsAndRotationFavoriteBtn();
		
		LampManager.getInstanceIfReady().addListener(this);
	}
	
	@Override
	public void onPause() {
		patScroller.clearListeners();
		
		LampManager.getInstanceIfReady().removeListener(this);
		
		fragmentVisible = false;
		
		// save anything that wasn't saved via swipe left
		patScroller.savePendingMoodConfigChanges();
		
		super.onPause();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate XML
        View v = inflater.inflate(R.layout.mood_fragment, container, false);

        // load in background image for this pattern
        final SlidingImageView patImage = (SlidingImageView) v.findViewById(R.id.patternImage);
        
        // load in the mood's image
        int imageID = getResources().getIdentifier(moodInfo.getImageName().replace(".jpg", ""), "drawable", getActivity().getApplicationContext().getPackageName());
        new ImageLoaderTask(getActivity(), imageID, patImage, null);
        
        // set title of pattern
        TextView titleText = (TextView) v.findViewById(R.id.patternName);
        titleText.setText(moodInfo.getName());
        
        // set description of pattern
        TextView descText = (TextView) v.findViewById(R.id.patternDesc);
        descText.setText(moodInfo.getDescription());
        
        // get reference to activity
        final Activity activity = this.getActivity();
        
        // get a reference to the favorite button
        favoriteBtn = (ImageView) v.findViewById(R.id.favoriteBtn);
        
        // attach listener to favoriteBtn being pressed
        favoriteBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (LampManager.getInstanceIfReady().getSelectedLampIfExists().getRotation().contains(moodInfo.getId())) {
					// this mood is in the lamp's rotation, so we are removing it
					LampManager.getInstanceIfReady().getSelectedLampIfExists().removeFromRotation(moodInfo.getId(), new SuccessFailCallback() {

						@Override
						public void onSuccess() {
							Toast.makeText(activity, "removed favorite", Toast.LENGTH_LONG).show();
						}

						@Override
						public void onFail() {
							favoriteBtn.setImageResource(R.drawable.favorited_on);
							Toast.makeText(activity, "failed to unfavorite", Toast.LENGTH_LONG).show();
						}
						
					});
					favoriteBtn.setImageResource(R.drawable.favorited_off);
				} else {
					// this mood is not in the lamp's rotation, so we are adding it
					LampManager.getInstanceIfReady().getSelectedLampIfExists().addToRotation(moodInfo.getId(), new SuccessFailCallback() {

						@Override
						public void onSuccess() {
							Toast.makeText(activity, "added favorite", Toast.LENGTH_LONG).show();
						}

						@Override
						public void onFail() {
							favoriteBtn.setImageResource(R.drawable.favorited_off);
							Toast.makeText(activity, "failed to favorite", Toast.LENGTH_LONG).show();
						}
						
					});
					favoriteBtn.setImageResource(R.drawable.favorited_on);
				}
			}
        });
        
        // get a reference to the restore button
        restoreBtn = (Button) v.findViewById(R.id.restoreBtn);
        
        // attach listener for save/restore buttons
        restoreBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
					l.restoreMoodConfigs(new SuccessFailCallback() {
						@Override
						public void onSuccess() {
							// since we just restored, we need to update the UI
							setStateOfConfigsAndRotationFavoriteBtn();
						}

						@Override
						public void onFail() {
							Toast.makeText(activity, "failed to restore", Toast.LENGTH_LONG).show();
						}
					});
				}
			}
        });
        
        // get a reference to both left/right scrollview layouts
        RelativeLayout patInfo = (RelativeLayout) v.findViewById(R.id.patternInfoLayout);
		ScrollView patConfigScroller = (ScrollView) v.findViewById(R.id.patternConfigScroller);
		LinearLayout patConfig = (LinearLayout) v.findViewById(R.id.patternConfigLayout);
		
		// get screen size
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		// set width of left panel
		LayoutParams infoLp = patInfo.getLayoutParams();
		infoLp.width = metrics.widthPixels;
		patInfo.setLayoutParams(infoLp);
		
		// set width of right panel
		LayoutParams configLp = patConfigScroller.getLayoutParams();
		configLp.width = metrics.widthPixels;
		patConfigScroller.setLayoutParams(configLp);
		
		// assign scrolling image to pattern scroller
		patScroller = (MoodScroller) v.findViewById(R.id.parentScroller);
		patScroller.setVerticalPagerToLock(verticalPager);
		patScroller.setSlidingImage(patImage);
		patScroller.setPageIndex(pageIndex);
		patScroller.setNumConfigs(moodInfo.getConfigs().size());
		
		final ImageView upArrow = (ImageView)getActivity().findViewById(R.id.upArrow);
		final ImageView rightArrow = (ImageView)getActivity().findViewById(R.id.rightArrow);
		final ImageView downArrow = (ImageView)getActivity().findViewById(R.id.downArrow);
		final ImageView leftArrow = (ImageView)getActivity().findViewById(R.id.leftArrow);
		
		patScroller.setArrows(upArrow, rightArrow, downArrow, leftArrow);
		patScroller.setFirstAndLast(first, last);
		
		// don't allow the mood to be scrolled if there are no configs
		if (moodInfo.getConfigs().size() == 0) {
			patScroller.setLocked();
		}
		
		// set up each individual config option
		for (int i=0; i<moodInfo.getConfigs().size(); i++) {
			final int index = i;
			final MoodConfigFromJSON config = moodInfo.getConfigs().get(index);
			
			// make sure we're actually displaying a widget
			if (!config.getWidget().equals("none")) {
				LinearLayout c = (LinearLayout)inflater.inflate(R.layout.mood_config, patConfig, false);
	
				TextView title = (TextView) c.findViewById(R.id.configTitle);
				title.setText(config.getName());
	
				// set label text or hide label if no text exists
				TextView label = (TextView) c.findViewById(R.id.configLabel);
				if (!config.getLabel().equals("")) {
					label.setText(config.getLabel());
					label.setGravity(Gravity.CENTER_HORIZONTAL);
				} else {
					label.setVisibility(View.GONE);
				}
	
				//Each possible config option has its own setup
				if (config.getWidget().equals("color")) {
					final HSVColorWheel colorWheel = new HSVColorWheel(getActivity());
					final MoodConfigFromJSON hueConfig = moodInfo.getConfigs().get(index);
					final MoodConfigFromJSON satConfig = moodInfo.getConfigs().get(index+1);
					
					// set default color on wheel
					setDefaultHueSat(hueConfig, satConfig, colorWheel);
					
					colorWheel.setId( 1 );
					
			        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)(metrics.widthPixels/1.75), (int)(metrics.widthPixels/1.75));
			        colorWheel.setLayoutParams(lp);
			        
			        colorWheel.setListener( new OnColorSelectedListener() {
						public void colorSelected(Integer color) {						
							float[] f = new float[3];
							Color.colorToHSV(color.intValue(), f);
							
							hueConfig.setCurrent((int)f[0]);
							satConfig.setCurrent((int)(f[1] * 255f));
							moodsFragment.setMoodConfigDual(moodInfo, hueConfig, satConfig, patScroller);
							
							// we have changes to save on swipe out
							if (patScroller.isRight())
								patScroller.setPendingSave();
						}
					} );
					
					c.addView( colorWheel, 1 );
					
					// store the view so we can update it later
					moodConfigViews.put(config.getId(), colorWheel);
					
				} else if(config.getWidget().equals("slider")) {
					final SeekBar seekBar = new SeekBar(getActivity());
					seekBar.setMax((int) (config.getMax() - config.getMin()));
					
			        LayoutParams lp = new LayoutParams((int)(metrics.widthPixels/1.75), LayoutParams.WRAP_CONTENT);
			        seekBar.setLayoutParams(lp);
			        
					seekBar.setProgress((int) config.getDefault());
					
					seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
						
						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}
						
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							config.setCurrent(progress + config.getMin());
							moodsFragment.setMoodConfig(moodInfo, config, patScroller);
							
							// we have changes to save on swipe out
							if (patScroller.isRight())
								patScroller.setPendingSave();
						}
					});
					
	
					seekBar.setOnTouchListener(new View.OnTouchListener() {
						@SuppressLint("ClickableViewAccessibility")
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							seekBar.getParent().requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});
					
					c.addView(seekBar, 1);
					
					// store the view so we can update it later
					moodConfigViews.put(config.getId(), seekBar);
					
				} else if(config.getWidget().equals("button")) {
					// for buttons we can hide their title textview
					title.setVisibility(View.GONE);
					
					final Button button = new Button(getActivity());
					button.setText(config.getName());
					button.setTextColor(Color.WHITE);
					
			        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			        button.setLayoutParams(lp);
					
					button.setOnClickListener(new Button.OnClickListener() {
						@Override
						public void onClick(View v) {
							moodsFragment.setMoodConfig(moodInfo, config, patScroller);
							
							// although it seems we wouldn't want to save on button presses,
							// buttons do in fact modify a mood (Light types for instance)
							
							// we have changes to save on swipe out
							if (patScroller.isRight())
								patScroller.setPendingSave();
						}
					});
					
					button.setOnTouchListener(new View.OnTouchListener() {
						@SuppressLint("ClickableViewAccessibility")
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							button.getParent().requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});
					
					c.addView(button, 1);
					
					// store the view so we can update it later
					moodConfigViews.put(config.getId(), button);
					
				} else if(config.getWidget().equals("toggle")) {
					final Switch toggle = new Switch(getActivity());
			        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			        toggle.setLayoutParams(lp);
					
			        toggle.setChecked(config.getDefault() != 0);
			        
			        toggle.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							int current = 0;
							if(isChecked)
								current = 1;
							config.setCurrent(current);
							moodsFragment.setMoodConfig(moodInfo, config, patScroller);
							
							// we have changes to save on swipe out
							if (patScroller.isRight())
								patScroller.setPendingSave();
						}
					});
	
					toggle.setOnTouchListener(new View.OnTouchListener() {
						@SuppressLint("ClickableViewAccessibility")
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							toggle.getParent().requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});
			        
			        c.addView(toggle, 1);
			        
			        // store the view so we can update it later
					moodConfigViews.put(config.getId(), toggle);
					
				} else if(config.getWidget().equals("time")) {
					final TimePickerDialog mTimePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
			            @Override
			            public void onTimeSet(TimePicker view, int hours, int minutes, int seconds) {
			            	config.setCurrent(seconds + (minutes * 60) + (hours * 3600));
			            	moodsFragment.setMoodConfig(moodInfo, config, patScroller);
			            }
			        }, 0, 1, 0, true);
			        
					final Button button = new Button(getActivity());
					button.setText("Set " + config.getName());
					button.setTextColor(Color.WHITE);
					
			        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			        button.setLayoutParams(lp);
					
					button.setOnClickListener(new Button.OnClickListener() {
						@Override
						public void onClick(View v) {
							mTimePicker.show();
						}
					});
					
					button.setOnTouchListener(new View.OnTouchListener() {
						@SuppressLint("ClickableViewAccessibility")
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							button.getParent().requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});
					
					c.addView(button, 1);
					
					// store the view so we can update it later
					moodConfigViews.put(config.getId(), button);
				}
				
				// layoutparams already set via XML
				patConfig.addView(c);
			}
		}
		
        return v;
    }

	@Override
	public void onLampListUpdate() {

	}

	@Override
	public void singleLampSelected(Lamp lamp) {
		if (fragmentVisible)
			setStateOfConfigsAndRotationFavoriteBtn();
	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {
		if (fragmentVisible)
			setStateOfConfigsAndRotationFavoriteBtn();
	}

	@Override
	public void noLampsSelected() {
		if (fragmentVisible)
			setStateOfConfigsAndRotationFavoriteBtn();
	}

	@Override
	public void onMoodChange(int moodId) {
		if (moodId == moodInfo.getId())
			setStateOfConfigsAndRotationFavoriteBtn();
	}

	@Override
	public void onWeatherLocationUpdate() {

	}

	@Override
	public void onDfuLampListUpdate() {

	}
}
