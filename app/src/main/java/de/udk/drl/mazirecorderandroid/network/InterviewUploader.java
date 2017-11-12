package de.udk.drl.mazirecorderandroid.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import de.udk.drl.mazirecorderandroid.models.AttachmentModel;
import de.udk.drl.mazirecorderandroid.models.InterviewModel;
import de.udk.drl.mazirecorderandroid.utils.Utils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Created by lutz on 04/11/16.
 */
public class InterviewUploader {


//    public static final String APP_BASE_URL = "http://local.mazizone.eu:9091/";
    public static final String APP_BASE_URL = "http://192.168.0.122:8081/";
    public static final String API_BASE_URL = APP_BASE_URL + "api/";

    public static final String API_DATE_FORMAT = "yyyy-MM-dd";

    public static class ServiceGenerator {

        private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        private static Retrofit.Builder builder =
                new Retrofit.Builder()
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .baseUrl(API_BASE_URL);

        public static <S> S createService(Class<S> serviceClass) {
            Retrofit retrofit = builder.client(httpClient.build()).build();
            return retrofit.create(serviceClass);
        }
    }

    public static class ConvertToJsonFunction implements Function<ResponseBody, JSONObject> {
        @Override
        public JSONObject apply(ResponseBody response) throws Exception {
            String string = response.string();
            return new JSONObject(string);
        }
    }

    public interface APIService {

        @POST("interviews")
        @FormUrlEncoded
        Observable<ResponseBody> postInterview(
                @Field("name") String title,
                @Field("role") String text,
                @Field("text") String date);

        @POST("attachments")
        @FormUrlEncoded
        Observable<ResponseBody> postAttachment(
                @Field("text") String text,
                @Field("tags") String[] tags,
                @Field("interview") String interview);

        @Multipart
        @POST("upload/image/{interviewId}")
        Observable<ResponseBody> uploadImage(
                @Path("interviewId") String interviewId,
                @Part MultipartBody.Part file
        );

        @Multipart
        @POST("upload/attachment/{attachmentId}")
        Observable<ResponseBody> uploadAttachment(
                @Path("attachmentId") String attachmentId,
                @Part MultipartBody.Part file
        );
    }

    public Context context;

    public InterviewUploader(Context context) {
        this.context = context;
    }

    public Observable<Boolean> postInterview(final InterviewModel interview) {

        final APIService service = ServiceGenerator.createService(APIService.class);

        // first post interview
        return service.postInterview(interview.name, interview.role, interview.text).map(new ConvertToJsonFunction())
//                .flatMap(new Function<JSONObject, Observable<ResponseBody>>() {
//                    // upload image
//                    @Override
//                    public Observable<ResponseBody> apply(JSONObject json) throws Exception {
//                        String id = json.getJSONObject("interview").getString("_id");
//
//                        if (interview.imageFile == null)
//                            return Observable.empty();
//                        File file = new File(interview.imageFile);
//
//                        if (!file.exists())
//                            return Observable.empty();
//
//                        MultipartBody.Part image = prepareFilePart("file", file);
//                        return service.uploadImage(id, image);
//                    }
//                }).map(new ConvertToJsonFunction())
                .flatMapIterable(new Function<JSONObject, Iterable<AttachmentModel>>() {
                    // create list of attachments
                    @Override
                    public Iterable<AttachmentModel> apply(JSONObject json) throws Exception {
                        Log.i("JSON",json.toString());
                        String id = json.getJSONObject("interview").getString("_id");
                        for (AttachmentModel attachments : interview.attachments)
                            attachments.interviewId = id;
                        return interview.attachments;
                    }
                })
                .flatMap(new Function<AttachmentModel, Observable<ResponseBody>>() {
                    // post attachments
                    @Override
                    public Observable<ResponseBody> apply(final AttachmentModel model) throws Exception {
                        return service.postAttachment(model.text,model.tags,model.interviewId)
                                .map(new ConvertToJsonFunction())
                                .flatMap(new Function<JSONObject, Observable<ResponseBody>>() {
                                    // upload attachment file for each attachment
                                    @Override
                                    public Observable<ResponseBody> apply(JSONObject json) throws Exception {
                                        String id = json.getString("_id");

                                        if (model.file == null)
                                            return Observable.empty();
                                        File file = new File(model.file);

                                        if (!file.exists())
                                            return Observable.empty();

                                        MultipartBody.Part filePart = prepareFilePart("file", file);
                                        return service.uploadAttachment(id, filePart);
                                    }
                                });
                    }
               }).toList()
                .map(new Function<List<ResponseBody>, Boolean>() {
                    // final check if everything went allright
                    @Override
                    public Boolean apply(List<ResponseBody> responseBodies) throws Exception {
                        return true;
                    }
                }).toObservable().subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());

    }

    private MultipartBody.Part prepareFilePart(String partName, File file) {

        //String type = context.getContentResolver().getType(Uri.fromFile(file));
        String type = Utils.getMimeType(context, Uri.fromFile(file));

        // create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse(type), file);

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

}
