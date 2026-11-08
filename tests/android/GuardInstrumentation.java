package com.instagram.minimal.tests;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import com.feurstagram.extension.*;

public class GuardInstrumentation extends Instrumentation {
    private int checks;
    private void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    public static class FakeFragment {
        private View view;
        private String module;
        FakeFragment(View view, String module) { this.view=view; this.module=module; }
        public View getView() { return view; }
        public String getModuleName() { return module; }
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            FixtureActivity a=(FixtureActivity)startActivitySync(new Intent(getTargetContext(),FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(() -> runChecks(a));
            result.putString("stream", "PASS "+checks+" Android guard checks\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable error) {
            result.putString("stream","FAIL "+error+"\n");
            finish(Activity.RESULT_CANCELED,result);
        }
    }
    private int id(Activity a,String name){return a.getResources().getIdentifier(name,"id",a.getPackageName());}
    private View child(Activity a,LinearLayout root,String name){View v=new View(a);v.setId(id(a,name));root.addView(v);return v;}
    private void runChecks(Activity a) {
        LinearLayout root=new LinearLayout(a);
        a.setContentView(root);
        View shared=child(a,root,"message_content_media_share_bubble_container");
        View dmPhoto=child(a,root,"direct_expiring_media_viewer_content");
        View notes=child(a,root,"cf_hub_recycler_view");
        View reels=child(a,root,"clips_tab");
        View home=child(a,root,"feed_tab");
        View search=child(a,root,"search_tab");
        View dm=child(a,root,"direct_tab");
        View profile=child(a,root,"profile_tab");
        View tabs=child(a,root,"profile_tabs_container");
        ContentGuard.installWindow(root);
        check(shared.getVisibility()==View.GONE,"shared post preview hidden");
        check(reels.getVisibility()==View.GONE,"reels entry hidden");
        check(home.getVisibility()==View.GONE,"feed entry hidden");
        check(tabs.getVisibility()==View.GONE,"profile media tabs hidden");
        check(notes.getVisibility()==View.VISIBLE,"Notes visible");
        check(dmPhoto.getVisibility()==View.VISIBLE,"direct photo visible");
        check(search.getVisibility()==View.VISIBLE,"account search entry visible");
        check(dm.getVisibility()==View.VISIBLE,"DM entry visible");
        check(profile.getVisibility()==View.VISIBLE,"profile entry visible");
        View viewer=new View(a);
        ContentGuard.onFragmentViewCreated(new FakeFragment(viewer,"clips_viewer_clips_tab"));
        check(viewer.getVisibility()==View.INVISIBLE,"cached reel fragment hidden");
        View story=new View(a);
        ContentGuard.onFragmentViewCreated(new FakeFragment(story,"reel_feed_timeline"));
        check(story.getVisibility()==View.INVISIBLE,"story fragment hidden");
        View account=new View(a);
        ContentGuard.onFragmentViewCreated(new FakeFragment(account,"edit_profile"));
        check(account.getVisibility()==View.VISIBLE,"account management visible");
        Intent blocked=new Intent(Intent.ACTION_VIEW,Uri.parse("https://instagram.com/p/ABC/"));
        NavigationGuard.filterIntent(blocked);
        check("instagram://direct-inbox".equals(blocked.getDataString()),"post deep link redirected");
        Intent wrapped=new Intent().putExtra("original_url","instagram://stories?user_id=1");
        NavigationGuard.filterIntent(wrapped);
        check("instagram://direct-inbox".equals(wrapped.getDataString()),"wrapped story link redirected");
        Intent user=new Intent(Intent.ACTION_VIEW,Uri.parse("instagram://user?username=dean"));
        NavigationGuard.filterIntent(user);
        check("instagram://user?username=dean".equals(user.getDataString()),"profile link preserved");
        check(NavigationGuard.filterIntent(null)==null,"null intent");
        a.getSharedPreferences("feurstagram_prefs",0).edit().putBoolean("block_reels",false).putBoolean("block_notes",true).commit();
        check(Config.getBlocked("block_reels",false),"old preferences cannot reveal reels");
        check(!Config.getBlocked("block_notes",true),"old preferences cannot hide Notes");
    }
}
