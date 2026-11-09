package com.instagram.minimal.tests;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
public class FixtureActivity extends Activity {
    Intent launchedIntent;
    @Override public void onCreate(Bundle state) { super.onCreate(state); }
    @Override public void startActivity(Intent intent) { launchedIntent = intent; }
}
