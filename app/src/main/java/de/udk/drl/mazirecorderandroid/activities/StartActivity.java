package de.udk.drl.mazirecorderandroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ViewSwitcher;

import de.udk.drl.mazirecorderandroid.R;
import de.udk.drl.mazirecorderandroid.models.InterviewStorage;

public class StartActivity extends BaseActivity {

    private ViewSwitcher switcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        switcher = (ViewSwitcher) findViewById(R.id.switcher);

        // reset all data
        InterviewStorage.getInstance(this).reset();

    }

    @Override
    protected void onResume() {
        super.onResume();
        switcher.reset();
        switcher.setDisplayedChild(0);
    }

    public void onSwitcherClicked(View view) {

        if (switcher.getDisplayedChild() == 0) {
            switcher.showNext();
        } else {
            Intent intent = new Intent(this, QuestionListActivity.class);
            startActivity(intent);
        }
    }
}
