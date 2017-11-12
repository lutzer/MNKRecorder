package de.udk.drl.mazirecorderandroid.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;

import java.io.File;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import de.udk.drl.mazirecorderandroid.R;
import de.udk.drl.mazirecorderandroid.models.InterviewModel;
import de.udk.drl.mazirecorderandroid.models.InterviewStorage;
import de.udk.drl.mazirecorderandroid.utils.Utils;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;



public class SynopsisActivity extends BaseActivity {

    public static final String PICTURE_FILES_DIRECTORY = "mazi_recorder";

    private static final int REQUEST_IMAGE_CAPTURE_CODE = 3;

    public File imageFile;
    private InterviewStorage interviewStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synopsis);

        // request permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_SYNOPSIS, REQUEST_SYNOPSIS_PERMISSIONS);
        }

        interviewStorage = InterviewStorage.getInstance(this);
        final EditText editTextName = (EditText) findViewById(R.id.edit_text_name);
        final EditText editTextRole = (EditText) findViewById(R.id.edit_text_role);

        final Button uploadButton = (Button) findViewById(R.id.upload_button);

        // set up rx patterns
        final Observable<CharSequence> editNameObservable = RxTextView.textChanges(editTextName);
        final Observable<CharSequence> editRoleObservable = RxTextView.textChanges(editTextRole);

        subscribers.add(
            Observable.combineLatest(editNameObservable, editRoleObservable,
                    new BiFunction<CharSequence, CharSequence, ArrayList<CharSequence>>() {
                        @Override
                        public ArrayList<CharSequence> apply(CharSequence charSequence1, CharSequence charSequence2) throws Exception {
                            ArrayList<CharSequence> list = new ArrayList();
                            list.add(charSequence1);
                            list.add(charSequence2);
                            return list;
                        }
            }).subscribe(new Consumer<ArrayList<CharSequence>>() {
                @Override
                public void accept(ArrayList<CharSequence> charSequences) throws Exception {
                    interviewStorage.interview.name = charSequences.get(0).toString();
                    interviewStorage.interview.role = charSequences.get(1).toString();
                    interviewStorage.save();
                }
            })
        );

        final CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox);

        final Observable<Boolean> checkBoxObservable = RxCompoundButton.checkedChanges(checkBox);

        //set upload button state
        subscribers.add(
            Observable.combineLatest(interviewStorage, checkBoxObservable, new BiFunction<InterviewModel, Boolean, Boolean>() {
                @Override
                public Boolean apply(InterviewModel interviewModel, Boolean checked) throws Exception {
                    return (interviewModel.name.length() >= MIN_INPUT_LENGTH
                            && checked
                    );
                }}
            ).subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean enable) throws Exception {
                    uploadButton.setEnabled(enable);
                }
            })
        );

    }

    public void onUploadButtonClicked(View view) {
        this.interviewStorage.save();

        Intent intent = new Intent(this, UploadActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //hide Spinner
        hideOverlay();

        if (requestCode == REQUEST_IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {

            if (imageFile.exists()) {
                interviewStorage.interview.imageFile = imageFile.getAbsolutePath();
                interviewStorage.save();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
