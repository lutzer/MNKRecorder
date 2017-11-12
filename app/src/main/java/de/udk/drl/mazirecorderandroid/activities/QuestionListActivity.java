package de.udk.drl.mazirecorderandroid.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.ArrayList;

import de.udk.drl.mazirecorderandroid.adapters.QuestionAdapter;
import de.udk.drl.mazirecorderandroid.models.AttachmentModel;
import de.udk.drl.mazirecorderandroid.models.InterviewModel;
import de.udk.drl.mazirecorderandroid.models.InterviewStorage;
import de.udk.drl.mazirecorderandroid.models.QuestionModel;
import de.udk.drl.mazirecorderandroid.models.QuestionStorage;

import de.udk.drl.mazirecorderandroid.R;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class QuestionListActivity extends BaseActivity {

    private QuestionStorage questionStorage;
    private InterviewStorage interviewStorage;
    private ListView questionListView;

    public static int ATTACHMENT_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_list);

        questionStorage = QuestionStorage.getInstance(this);
        interviewStorage = InterviewStorage.getInstance(this);

        //setup listview
        questionListView = (ListView) findViewById(R.id.questionList);

        TextView emptyText = (TextView)findViewById(R.id.empty_text);
        questionListView.setEmptyView(emptyText);

        // update list on question change
        subscribers.add(
            Observable.combineLatest(questionStorage, interviewStorage,
                new BiFunction<ArrayList<QuestionModel>, InterviewModel, ArrayList<QuestionModel>>() {
                    @Override
                    public ArrayList<QuestionModel> apply(ArrayList<QuestionModel> questionModels, InterviewModel interviewModel) throws Exception {
                        return questionModels;
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<ArrayList<QuestionModel>>() {
                    @Override
                    public void accept(ArrayList<QuestionModel> questionModels) throws Exception {
                        QuestionAdapter adapter = new QuestionAdapter(QuestionListActivity.this, R.layout.item_question, questionModels);
                        questionListView.setAdapter(adapter);
                    }
                })
        );

        // activate upload button only when there is at least one recording
        final Button uploadButton = (Button) findViewById(R.id.synopsis_button);
        subscribers.add(
                interviewStorage.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<InterviewModel>() {
                    @Override
                    public void accept(InterviewModel model) {
                        uploadButton.setEnabled(model.attachments.size() > 0);
                    }
                })
        );
    }

    public void onDeleteButtonClicked(final View view) {

        interviewStorage.reset();
        finish();

    }

    public void onItemDeleteButtonClicked(View view) {
        int index = (int)view.getTag();
        questionStorage.delete(index);
    }

    public void onListItemClicked(View view) {
        String question = (String)view.getTag();
        Intent intent = new Intent(getApplicationContext(), RecorderActivity.class);
        intent.putExtra("question", question);
        startActivityForResult(intent,ATTACHMENT_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ATTACHMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            if (data.hasExtra("attachment")) {
                AttachmentModel attachment = (AttachmentModel) data.getSerializableExtra("attachment");

                if (interviewStorage.interview.attachments.contains(attachment))
                    interviewStorage.interview.attachments.remove(attachment);

                interviewStorage.interview.attachments.add(attachment);
                interviewStorage.save();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onSynopsisButtonClicked(View view) {
        Intent intent = new Intent(getApplicationContext(), SynopsisActivity.class);
        startActivity(intent);
    }
}
