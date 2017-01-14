package uk.co.deanwild.materialshowcaseviewsample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class SimpleSingleExample extends AppCompatActivity implements View.OnClickListener {

    private Button mButtonShow;
    private Button mButtonReset;

    private static final String SHOWCASE_ID = "simple example";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_single_example);
        mButtonShow = (Button) findViewById(R.id.btn_show);
        mButtonShow.setOnClickListener(this);

        mButtonReset = (Button) findViewById(R.id.btn_reset);
        mButtonReset.setOnClickListener(this);

        presentShowcaseView(1000); // one second delay
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_show) {

            presentShowcaseView(0);

        } else if (v.getId() == R.id.btn_reset) {

            MaterialShowcaseView.resetSingleUse(this, SHOWCASE_ID);
            Toast.makeText(this, "Showcase reset", Toast.LENGTH_SHORT).show();
        }

    }

    private void presentShowcaseView(int withDelay) {
        TextView textView = getCustomContentTextView();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        new MaterialShowcaseView.Builder(this)
                .setTarget(mButtonShow)
                .setTargetTouchable(true)
                .setContentTextView(textView)
                .showActionIcon(true)
                .setActionIcon(bitmap)
                .setDelay(withDelay) // optional but starting animations immediately in onCreate can make them choppy
                .shouldContentStartFromTargetCenter(true)
                .show();
    }

    @NonNull
    private TextView getCustomContentTextView() {
        TextView textView = new TextView(this);
        textView.setText("This is a custom text view");
        textView.setTextColor(ContextCompat.getColor(this, R.color.green));
        textView.setBackgroundColor(ContextCompat.getColor(this, R.color.darkred));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(10, 10, 10, 10);
        textView.setLayoutParams(layoutParams);
        textView.setPadding(30, 10, 30, 10);
        return textView;
    }


}
