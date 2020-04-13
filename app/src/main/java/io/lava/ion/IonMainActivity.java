package io.lava.ion;

import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.fragments.about.AboutFragment;
import io.lava.ion.fragments.home.HomeFragment;
import io.lava.ion.fragments.home.lamplist.LampListItem;
import io.lava.ion.fragments.notifications.NotificationsFragment;
import io.lava.ion.fragments.settings.SettingsFragment;
import io.lava.ion.fragments.update.UpdateDialogManager;
import io.lava.ion.fragments.update.UpdateFragment;
import io.lava.ion.moods.MoodManager;
import io.lava.ion.moods.MoodsFragment;
import io.lava.ion.moods.NotificationManager;
import io.lava.ion.receivers.AlarmScheduler;

import java.util.ArrayList;
import java.util.Arrays;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

@SuppressWarnings("unchecked")
public class IonMainActivity extends FragmentActivity implements ILampManagerListener {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerListLeft;
	private ListView mDrawerListRight;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mTitle;
	private ArrayList<String> mLeftMenuOptionTitles;
	private Fragment currentFragment;
	
	private boolean leftDrawerOpen = false;
	private boolean rightDrawerOpen = false;

	private BroadcastReceiver bluetoothAlert;
	
	private LampGUIStateManager lampGuiStateManager;

    private int currentFragmentIndex = -1;

	private static final String[] LOCATION_PERMS = { Manifest.permission.ACCESS_COARSE_LOCATION };
	private static final int LOCATION_REQUEST_CODE = 1337;

	private boolean canAccessLocation () {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}
	
	@Override
	protected void onPause() {
		LampManager.getInstanceIfReady().setLampDiscoveryContinuous(false);
		
		// update visibility status
		UpdateDialogManager.getInstance(getApplicationContext()).appBecameHidden();

        // allow fragments to be created even if the same (because we were paused)
        currentFragmentIndex = -1;
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// update visibility status
		UpdateDialogManager.getInstance(getApplicationContext()).appBecameVisible();
		
		if (rightDrawerOpen) {
			LampManager.getInstanceIfReady().setLampDiscoveryContinuous(true);
		}
		
		// look for any lamps that we should connect to
    	if (LampManager.createInstanceIfNeeded(getApplicationContext()).shouldBackgroundDiscoveryRun()) {
    		LampManager.createInstanceIfNeeded(getApplicationContext()).discoverLamps();
    	}

        // make sure our lamp status (and notification control) is still relevant
        LampManager.getInstanceIfReady().refreshLampStatus();

        if (!canAccessLocation()) {
            requestPermissions(LOCATION_PERMS, LOCATION_REQUEST_CODE);
        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// create the lamp manager in all its application context glory (tied to the system process)
		LampManager.createInstanceIfNeeded(getApplicationContext());
		
		// ensure our alarms are running
        AlarmScheduler.setupAlarms(getApplicationContext());
		
		setContentView(R.layout.activity_ion_main);
		
		LampManager.getInstanceIfReady().addListener(this);
		
		LampManager.getInstanceIfReady().setMainActivity(this);
		
		// initialize mood/notification managers
		MoodManager.getInstance(this);
		NotificationManager.getInstance(this);

		// initialize drawers / action bar
		mTitle = getTitle();
		mLeftMenuOptionTitles = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.left_menu_drawer_options_array)));
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		mDrawerListLeft = (ListView) findViewById(R.id.left_drawer);
		mDrawerListRight = (ListView) findViewById(R.id.right_drawer);
		
		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// set up the drawer's list view with items and click listener
		mDrawerListLeft.setAdapter(new FragmentListAdapter(this, mLeftMenuOptionTitles));
		mDrawerListLeft.setOnItemClickListener(new LeftDrawerItemClickListener());

		// The arraylist here will (should) be replaced later in receivedManager()
		mDrawerListRight.setOnItemClickListener(new RightDrawerItemClickListener());
		mDrawerListRight.setOnItemLongClickListener(new RightDrawerItemClickListener());
		
		// the lamp state manager handles updating the action bar's lamp spinner
		lampGuiStateManager = new LampGUIStateManager(this, mDrawerListRight, LampManager.getInstanceIfReady());
		 
		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions 
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, // host Activity 
		mDrawerLayout, // DrawerLayout object
		R.drawable.ic_drawer, // nav drawer image to replace 'Up' caret 
		R.string.drawer_open, // "open drawer" description for accessibility 
		R.string.drawer_close // "close drawer" description for accessibility 
		) {
			public void onDrawerClosed(View drawerView) {
				// only animate drawer "3 line" icon if left drawer is being manipulated
				if (drawerView == mDrawerListLeft)
					super.onDrawerClosed(drawerView);
				
				if (drawerView == mDrawerListRight) {
					LampManager.getInstanceIfReady().setLampDiscoveryContinuous(false);
					
					rightDrawerOpen = false;
				} else if (drawerView == mDrawerListLeft) {
					leftDrawerOpen = false;
				}
				
				if (!leftDrawerOpen && !rightDrawerOpen) {
					// lock the drawer again
					mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
				}
				
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				// only animate drawer "3 line" icon if left drawer is being manipulated
				if (drawerView == mDrawerListLeft)
					super.onDrawerOpened(drawerView);
				
				if (drawerView == mDrawerListRight) {
					rightDrawerOpen = true;
					
					// always ensure we start discovering as soon as the right drawer is opened
					LampManager.getInstanceIfReady().setLampDiscoveryContinuous(true);
				} else if (drawerView == mDrawerListLeft) {
					leftDrawerOpen = true;
				}
				
				// allow swipe to close
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			}

			public void onDrawerSlide(final View drawerView, final float slideOffset) {
				// only animate drawer "3 line" icon if left drawer is being manipulated
				if (drawerView == mDrawerListLeft)
					super.onDrawerSlide(drawerView, slideOffset);

				// The direction that the drawer comes out, based on which drawer this is
				final int scrollDirection;

				if (drawerView == mDrawerListRight)
					scrollDirection = -1;
				else
					scrollDirection = 1;

				// moves the main activity as the drawer pulls out
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Point size = new Point();
						getWindowManager().getDefaultDisplay().getSize(size);

						View v = currentFragment.getView();
						if (v != null)
							v.setTranslationX(scrollDirection * slideOffset *  drawerView.getWidth());
					}
				});
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// This innocuous-looking little tidbit actually initializes the home fragment
		selectFragment(0);

		// This will be displayed if bluetooth is disabled or if it gets disabled
		final LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main_layout);
		final LinearLayout bluetoothWarningBanner = (LinearLayout) findViewById(R.id.bluetoothWarning);
		
		// opens the bluetooth settings menu
		bluetoothWarningBanner.setOnClickListener(new TextView.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intentOpenBluetoothSettings = new Intent();
				intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
				startActivity(intentOpenBluetoothSettings);				
			}
		});
		
		// remove the banner if bluetooth is ready to go
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
			mainLayout.removeView(bluetoothWarningBanner);
		}
		
		//This listens to the state of bluetooth and makes an alert if bluetooth gets disabled.
		//It may make more sense to make this appear as a notification since the app is pretty much useless without it
		bluetoothAlert = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		        final String action = intent.getAction();
		        
		        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
		            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		            switch (state) {
		            case BluetoothAdapter.STATE_OFF:
		            	// If the warning isn't displayed, show it
		            	if(mainLayout != bluetoothWarningBanner.getParent())
		            		mainLayout.addView(bluetoothWarningBanner, 0);
		                break;
		            case BluetoothAdapter.STATE_ON:
		            	// If the warning is displayed, get rid of it
		            	if(mainLayout == bluetoothWarningBanner.getParent())
		            		mainLayout.removeView(bluetoothWarningBanner);
		                break;
		            }
		        }
		    }
		};

		// Since this receiver is anonymous, it must be registered at run time
		this.registerReceiver(bluetoothAlert, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	}
	
	private void handleLampDrawerOpenClose() {
		if (!rightDrawerOpen) {
			if (leftDrawerOpen) {
				mDrawerLayout.closeDrawer(mDrawerListLeft);
			}
			
			mDrawerLayout.openDrawer(mDrawerListRight);
		} else {
			mDrawerLayout.closeDrawer(mDrawerListRight);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			// left drawer is opening (or closing), make sure right drawer is closed
			if (rightDrawerOpen) {
				mDrawerLayout.closeDrawer(mDrawerListRight);
			}
			
			return true;
		}
		// Handle action buttons
		switch (item.getItemId()) {
			case R.id.action_lamp_menu:
				handleLampDrawerOpenClose();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private Context getContext() {
		return this;
	}

	/* The click listener for ListView in the navigation drawer */
	private class LeftDrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectFragment(position);
		}
	}

	/* The click listner for ListView in the navigation drawer */
	private class RightDrawerItemClickListener implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mDrawerListRight.clearChoices();
			((ArrayAdapter<Lamp>) mDrawerListRight.getAdapter()).notifyDataSetChanged();
			
			LampListItem lampListItem = (LampListItem)mDrawerListRight.getAdapter().getItem(position);
			
			// make sure the user didn't tap the header
			if (lampListItem instanceof Lamp) {
				Lamp l = (Lamp)lampListItem;
				if (l.getConnectivityState() == Lamp.LampState.DISCONNECTED)
					l.connect();
				else
					l.disconnect();
			}
		}
		
		// allow lamps to be forgotten
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long id) {
			LampListItem lampListItem = (LampListItem)mDrawerListRight.getAdapter().getItem(position);
			
			// make sure the user didn't tap the header
			if (lampListItem instanceof Lamp) {
				final Lamp lamp = (Lamp)lampListItem;

				final boolean ready = lamp.getConnectivityState() == Lamp.LampState.READY;
				final boolean seenRecently = lamp.seenRecently();
				final boolean remembered = lamp.isRemembered();
				
				final CharSequence[] items;
				
				if (ready) {
					// ready
					items = new CharSequence[]{ "Disconnect", "Cancel" };
				} else {
					// not ready
					if (seenRecently) {
						// not ready, seen recently
						if (remembered) {
							// not ready, seen recently, remembered
							
							// app is connecting/initializing with lamp
							items = new CharSequence[]{ "Disconnect", "Cancel" };
						} else {
							// not ready, seen recently, not remembered
							items = new CharSequence[]{ "Connect", "Cancel" };
						}
					} else {
						// not ready, not seen recently, remembered
						items = new CharSequence[]{ "Forget", "Cancel" };
					}
				}

	            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

	            builder.setTitle(lamp.getName());
	            builder.setItems(items, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int item) {
	                	if (items[item].equals("Forget")) {
	                		lamp.forget();
		                    
		                    if (LampManager.getInstanceIfReady() != null) {
		            			LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
		            		}
	                	} else if (items[item].equals("Connect")) {
	                		lamp.connect();
	                	} else if (items[item].equals("Disconnect")) {
	                		lamp.disconnect();
	                	} else {
	                		// cancel
	                		dialog.dismiss();
	                	}
	                }
	            });

	            AlertDialog alert = builder.create();
	            alert.show();
	            
	            return true;
			} else {
				return false;
			}
		}
	}

	public void selectFragment(int position) {
        // don't recreate fragments
        if (position == currentFragmentIndex)
            return;

        currentFragmentIndex = position;

		currentFragment = getFragmentForPosition(position);

		mDrawerListLeft.setItemChecked(position, true);
		setTitle(mLeftMenuOptionTitles.get(position));
		mDrawerLayout.closeDrawer(mDrawerListLeft);
		getFragmentManager().beginTransaction().replace(R.id.content_frame, currentFragment).commit();
	}

	private Fragment getFragmentForPosition(int position) {
		switch (position) {
			case 0: {
				return new HomeFragment();
			}
			case 1: {
				return MoodsFragment.newInstance(getSupportFragmentManager());
			}
			case 2: {
				return new NotificationsFragment();
			}
			case 3: {
				return new SettingsFragment();
			}
			case 4: {
				return new UpdateFragment();
			}
			case 5: {
				return new AboutFragment();
			}
		}
		return new Fragment();
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onDestroy(){
		LampManager.getInstanceIfReady().removeListener(this);
		
		this.unregisterReceiver(bluetoothAlert);
		
		lampGuiStateManager.onDestroy();
		
		super.onDestroy();
	}

	@Override
	public void onLampListUpdate() {
		// update right drawer lamp list
		ArrayAdapter<Lamp> lampAdapter = (ArrayAdapter<Lamp>)mDrawerListRight.getAdapter();
		lampAdapter.notifyDataSetChanged();
	}

	@Override
	public void singleLampSelected(Lamp lamp) {

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