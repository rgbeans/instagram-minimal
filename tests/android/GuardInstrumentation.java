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
        boolean resumed = true;
        FakeFragment(View view, String module) { this.view=view; this.module=module; }
        public View getView() { return view; }
        public String getModuleName() { return module; }
        public boolean isResumed() { return resumed; }
    }
    private static class UnreadableExtrasIntent extends Intent {
        UnreadableExtrasIntent(String url) { super(Intent.ACTION_VIEW, Uri.parse(url)); }
        @Override public Bundle getExtras() { throw new android.os.BadParcelableException("Unreadable foreign extras"); }
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
        LinearLayout searchRoot = new LinearLayout(a);
        ListView results = new ListView(a);
        LinearLayout[] rows = {new LinearLayout(a), new LinearLayout(a)};
        child(a, rows[0], "row_search_user_username");
        rows[1].addView(new View(a));
        rows[1].setClickable(true);
        rows[1].setLongClickable(true);
        rows[1].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        results.setAdapter(new BaseAdapter() {
            public int getCount() { return rows.length; }
            public Object getItem(int position) { return rows[position]; }
            public long getItemId(int position) { return position; }
            public View getView(int position, View convert, android.view.ViewGroup parent) {
                rows[position].setMinimumHeight(96);
                return rows[position];
            }
        });
        searchRoot.addView(results);
        a.setContentView(searchRoot);
        results.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        results.layout(0, 0, 400, 400);
        check(results.getChildCount()==2,"search fixture contains both result types");
        ContentGuard.onFragmentViewCreated(new FakeFragment(searchRoot,"search_typeahead"));
        check(rows[0].getVisibility()==View.VISIBLE,"account result visible");
        check(rows[1].getVisibility()==View.GONE,"unknown media result hidden");
        rows[1].removeAllViews();
        child(a, rows[1], "row_search_user_username");
        searchRoot.getViewTreeObserver().dispatchOnPreDraw();
        check(rows[1].getVisibility()==View.VISIBLE,"recycled account result restored");
        check(rows[1].isClickable(),"recycled account result remains clickable");
        check(rows[1].isLongClickable(),"recycled account result retains long click");
        check(rows[1].getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_YES,
                "recycled account result remains accessible");
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
        Intent foreignPost = new UnreadableExtrasIntent("https://instagram.com/p/ABC/");
        NavigationGuard.filterIntent(foreignPost);
        check("instagram://direct-inbox".equals(foreignPost.getDataString()),"unreadable extras cannot bypass a media URL block");
        Intent foreignAccount = new UnreadableExtrasIntent("instagram://user?username=dean");
        NavigationGuard.filterIntent(foreignAccount);
        check("instagram://user?username=dean".equals(foreignAccount.getDataString()),"unreadable extras preserve an allowed account URL");
        a.getSharedPreferences("feurstagram_prefs",0).edit().putBoolean("block_reels",false).putBoolean("block_notes",true).commit();
        check(Config.getBlocked("block_reels",false),"old preferences cannot reveal reels");
        check(!Config.getBlocked("block_notes",true),"old preferences cannot hide Notes");
        verifyChatControls((FixtureActivity) a);
    }

    private void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 300, 500);
    }

    private void verifyChatControls(FixtureActivity a) {
        android.view.Window window = a.getWindow();
        int secure = android.view.WindowManager.LayoutParams.FLAG_SECURE;
        LinearLayout inbox = new LinearLayout(a);
        LinearLayout header = new LinearLayout(a);
        header.setId(id(a, "direct_inbox_action_bar"));
        LinearLayout actions = new LinearLayout(a);
        actions.setId(id(a, "action_bar_buttons_container_right"));
        ImageButton compose = new ImageButton(a);
        compose.setOnClickListener(v -> {});
        actions.addView(compose, new LinearLayout.LayoutParams(48, 48));
        header.addView(actions);
        inbox.addView(header);
        a.setContentView(inbox);
        layout(inbox);
        FakeFragment inboxFragment = new FakeFragment(inbox, "direct_inbox");
        ContentGuard.onFragmentViewCreated(inboxFragment);
        check(actions.getChildCount()==2 && actions.getChildAt(0)==compose && compose.isClickable(), "bell added beside compose");
        View bell = actions.getChildAt(1);
        check(bell instanceof ImageButton && ((ImageButton) bell).getDrawable()!=null
                && "Notifications".contentEquals(bell.getContentDescription()), "accessible notifications icon");
        inbox.getViewTreeObserver().dispatchOnPreDraw();
        check(actions.getChildCount()==2,"one bell after repeated draws");
        bell.performClick();
        check(a.launchedIntent==null && inboxFragment.resumed,
                "bell keeps the inbox in place when a native session is unavailable");
        check(ScreenCapture.skipRequest(window,"DirectInboxController"),"ordinary inbox permits capture");

        inboxFragment.resumed = false;
        inbox.getViewTreeObserver().dispatchOnPreDraw();
        check(actions.getChildCount()==1 && !ScreenCapture.skipRequest(window,"DirectThreadController"),"bell and capture override leave with chat");
        window.addFlags(secure); // native manager applies the request recorded above
        LinearLayout thread = new LinearLayout(a);
        a.setContentView(thread);
        layout(thread);
        FakeFragment threadFragment = new FakeFragment(thread,"direct_thread");
        ContentGuard.onFragmentViewCreated(threadFragment);
        check((window.getAttributes().flags & secure)==0,"ordinary thread clears a residual chat flag");
        check(!ScreenCapture.skipRequest(window,"DirectVisualMessageViewerController")
                && (window.getAttributes().flags & secure)!=0,"view-once capture protection restored immediately");
        check(ScreenCapture.skipRelease(window,"DirectInboxController")
                && !ScreenCapture.skipRelease(window,"DirectVisualMessageViewerController"),
                "skipped ordinary request cannot release a protected request");
        View disappearing = child(a,thread,"disappearing_mode_indicator_placeholder");
        disappearing.setMinimumWidth(48);
        disappearing.setMinimumHeight(48);
        layout(thread);
        thread.getViewTreeObserver().dispatchOnPreDraw();
        check(!ScreenCapture.skipRequest(window,"DirectThreadController")
                && (window.getAttributes().flags & secure)!=0,"disappearing chat stays protected");
        disappearing.setVisibility(View.GONE);
        thread.getViewTreeObserver().dispatchOnPreDraw();
        check((window.getAttributes().flags & secure)==0,"ordinary capture restored after protected content closes");

        View card = new View(a);
        card.setClickable(true);
        thread.addView(card);
        SharedMessages.update(card,MinimalPolicy.blocksSharedMessage("XMA_MEDIA_SHARE"));
        card.setVisibility(View.VISIBLE); // native binder restores visibility on its own
        thread.getViewTreeObserver().dispatchOnPreDraw();
        check(card.getVisibility()==View.GONE,"shared post remains hidden before drawing");
        SharedMessages.update(card,MinimalPolicy.blocksSharedMessage("MEDIA"));
        check(card.getVisibility()==View.VISIBLE && card.isClickable(),"recycled direct attachment stays usable");
        LinearLayout richCard = new LinearLayout(a);
        richCard.setId(id(a,"standard_dxma_message_container"));
        thread.addView(richCard);
        child(a,richCard,"message_content_media_share_bubble_container");
        thread.getViewTreeObserver().dispatchOnPreDraw();
        check(richCard.getVisibility()==View.GONE,"shared post container and its controls are hidden");
        richCard.removeAllViews();
        richCard.addView(new TextView(a));
        thread.getViewTreeObserver().dispatchOnPreDraw();
        check(richCard.getVisibility()==View.VISIBLE,"ordinary rich cards remain visible after recycling");
        threadFragment.resumed = false;
        thread.getViewTreeObserver().dispatchOnPreDraw();
        check((window.getAttributes().flags & secure)!=0,"native capture flag restored on leaving chat");
        ScreenCapture.skipRelease(window,"DirectThreadController");
        window.clearFlags(secure);
    }
}
