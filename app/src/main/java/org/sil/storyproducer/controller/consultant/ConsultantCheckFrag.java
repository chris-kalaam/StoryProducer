package org.sil.storyproducer.controller.consultant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.dramatization.DramatizationFrag;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.FileSystem;

import java.io.File;

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
public class ConsultantCheckFrag extends Fragment {

    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    public static final String CONSULTANT_PREFS = "Consultant_Checks";
    public static final String IS_CONSULTANT_APPROVED = "isApproved";
    private static final String IS_CHECKED = "isChecked";
    private String STORY_NAME = "storyname";
    private String storyName;
    private int slidePosition;
    private View rootView;
    private boolean isChecked;
    private AudioPlayer draftPlayer;
    private SlideText slideText;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_con_check, container, false);
        storyName = StoryState.getStoryName();
        slideText = FileSystem.getSlideText(storyName, slidePosition);

        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_concheck_image_view), slidePosition);
        setScriptureText((TextView)rootView.findViewById(R.id.fragment_concheck_scripture_text));
        setReferenceText((TextView)rootView.findViewById(R.id.fragment_concheck_reference_text));
        setDraftPlaybackButton((ImageButton)rootView.findViewById(R.id.concheck_draft_playback_button));
        setCheckmarkButton((ImageButton)rootView.findViewById(R.id.concheck_checkmark_button));
        setLogsButton((ImageButton)rootView.findViewById(R.id.concheck_logs_button));

        return rootView;
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors(){
        if(slidePosition == 0){
            RelativeLayout rl =  (RelativeLayout)rootView.findViewById(R.id.concheck_relative_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout)rootView.findViewById(R.id.concheck_button_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            TextView tv = (TextView) rootView.findViewById(R.id.fragment_concheck_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = (TextView) rootView.findViewById(R.id.fragment_concheck_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            ImageButton ib = (ImageButton) rootView.findViewById(R.id.fragment_concheck_narration_button);
            ib.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(ImageView slideImage, int slideNum) {
        Bitmap slidePicture = FileSystem.getImage(StoryState.getStoryName(), slideNum);

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.3;
        height = (int)(height * scalingFactor);

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }


    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private void setScriptureText(TextView textView) {

        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private void setReferenceText(TextView textView) {
        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }
    }

    /**
     * This function sets the draft playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     * @param button the ImageButton view handler to set the onclicklistener to
     */
    private void setDraftPlaybackButton(ImageButton button) {

        final File draftFile = FileSystem.getTranslationAudio(StoryState.getStoryName(), slidePosition);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop other playback streams.
                if (draftPlayer != null && draftPlayer.isAudioPlaying()) {
                    draftPlayer.stopAudio();
                }
                if (draftFile.exists()) {
                    draftPlayer = new AudioPlayer();
                    draftPlayer.playWithPath(draftFile.getPath());
                    Toast.makeText(getContext(), "Playing Draft Audio...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No Draft Audio Found...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setCheckmarkButton(final ImageButton button) {
        final SharedPreferences prefs = getActivity().getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        final String prefsKeyString = storyName + slidePosition + IS_CHECKED;
        isChecked = prefs.getBoolean(prefsKeyString, false);
        if(isChecked) {
            button.setBackgroundResource(R.drawable.ic_checkmark_green);
        } else {
            button.setBackgroundResource(R.drawable.ic_checkmark_red);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isChecked) {
                    button.setBackgroundResource(R.drawable.ic_checkmark_red);
                    isChecked = false;
                    prefsEditor.putBoolean(prefsKeyString, false);
                    prefsEditor.apply();
                } else {
                    button.setBackgroundResource(R.drawable.ic_checkmark_green);
                    isChecked = true;
                    prefsEditor.putBoolean(prefsKeyString, true);
                    prefsEditor.commit();
                    if(checkAllMarked()) {
                        showConsultantPasswordDialog();
                    }
                }
            }
            //TODO: check all of stories isChecked to see if story is completely checked
            // if story is checked, launch password to unlock dramatization
        });
    }

    private void setLogsButton(ImageButton button) {
        button.setBackgroundResource(R.drawable.ic_logs_blue);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Log interface yet to be implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkAllMarked() {
        boolean marked;
        SharedPreferences prefs = getActivity().getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE);
        int numStorySlides = FileSystem.getTotalSlideNum(storyName);
        for (int i = 0; i < numStorySlides; i ++) {
            marked = prefs.getBoolean(storyName + i + IS_CHECKED, false);
            if (!marked) {
                return false;
            }
        }
        return true;
    }

    private void showConsultantPasswordDialog() {
        final EditText password = new EditText(getContext());
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Programmatically set layout properties for edit text field
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        // Apply layout properties
        password.setLayoutParams(params);
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.consultant_password_title))
                .setMessage(getString(R.string.consultant_password_message))
                .setView(password)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.submit), null)
                .create();
        // This is set to dismiss the keyboard manually on dialog dismiss
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                toggleKeyboard(false, getView());
            }
        });

        // This manually sets the submit button listener so that the dialog doesn't always submit
        // If the password is incorrect, we want to stay on the dialog and give an error message
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String passwordText = password.getText().toString();
                        if (passwordText.contentEquals("magic password")) {
                            saveConsultantApproval();
                            dialog.dismiss();
                            launchDramatizationPhase();
                        } else {
                            password.setError(getString(R.string.consultant_incorrect_password_message));
                        }
                    }
                });
            }
        });

        dialog.show();
        toggleKeyboard(true, password);
    }

    /**
     * Updates the shared preference file to mark the story as approved
     */
    private void saveConsultantApproval() {
        SharedPreferences.Editor prefsEditor = getActivity().getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE).edit();
        prefsEditor.putBoolean(storyName + IS_CONSULTANT_APPROVED, true);
        prefsEditor.apply();
    }

    /**
     * Launches the dramatization phase for the story and starts back at first slide
     */
    private void launchDramatizationPhase() {
        Toast.makeText(getContext(), "Congrats!", Toast.LENGTH_SHORT).show();
        int dramatizationPhaseIndex = 4;
        Phase[] phases = StoryState.getPhases();
        StoryState.setCurrentPhase(phases[dramatizationPhaseIndex]);
        Intent intent = new Intent(getContext(), phases[dramatizationPhaseIndex].getTheClass());
        intent.putExtra(STORY_NAME, storyName);
        intent.putExtra(SLIDE_NUM, 0);
        getActivity().startActivity(intent);
    }

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private void toggleKeyboard(boolean showKeyBoard, View aView) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (showKeyBoard) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else {
            imm.hideSoftInputFromWindow(aView.getWindowToken(), 0);
        }
    }

}
