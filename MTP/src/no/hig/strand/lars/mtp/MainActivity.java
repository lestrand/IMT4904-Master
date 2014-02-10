package no.hig.strand.lars.mtp;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {

	private TabsPagerAdapter mTabsPagerAdapter;
	private ViewPager mViewPager;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mTabsPagerAdapter);
        
        setupUI();
    }


    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case R.id.action_new_list:
			startActivity(new Intent(this, ListActivity.class));
			return true;
		case R.id.action_settings:
			return true;
		case R.id.action_about:
			return true;
		}
		return super.onOptionsItemSelected(item);
	}



	private void setupUI() {
    	final ActionBar actionBar = getActionBar();
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    	
    	ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {}
			
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				mViewPager.setCurrentItem(tab.getPosition());
			}
			
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {}
		};
		
		actionBar.addTab(actionBar.newTab().setText(R.string.today)
				.setTabListener(tabListener));
		actionBar.addTab(actionBar.newTab().setText(R.string.week)
				.setTabListener(tabListener));
		actionBar.addTab(actionBar.newTab().setText(R.string.all_tasks)
				.setTabListener(tabListener));
		
		mViewPager.setOnPageChangeListener(
				new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				getActionBar().setSelectedNavigationItem(position);
			}
		});
		
    }
    
    
    
    public class TabsPagerAdapter extends FragmentStatePagerAdapter {

    	
		public TabsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		
		
		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case 0: return new TodayFragment();
			case 1: return new WeekFragment();
			case 2: return new AllTasksFragment();
			default: return new TodayFragment();
			}
		}

		
		
		@Override
		public int getCount() {
			return 3;
		}
    	
    }
    
    
}
