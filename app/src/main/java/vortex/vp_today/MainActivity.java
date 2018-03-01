package vortex.vp_today;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private String date = "";
    private TextView textView;
    private TextView msgOTD;
    private TextView tvVers;
    private volatile String tmp = "";
    private Thread t;
    private Button btnDate;
    private SwipeRefreshLayout swipe;

    /* Temporäre Anzeige des HTML Quelltextes der abgerufenden Seite -> Endeffekt wirds eine Listview werden */
    EditText txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initialize views */
        txt = findViewById(R.id.text);
        textView = findViewById(R.id.textView);
        msgOTD = findViewById(R.id.msgOTD);
        btnDate = findViewById(R.id.btnDate);
        swipe = findViewById(R.id.swiperefresh);
        tvVers = findViewById(R.id.tvVers);

        /* Thread region */
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (date.equals("")) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Ein Fehler ist während des Aktualisiervorgangs aufgetreten!", Toast.LENGTH_LONG);
                            }
                        });
                        return;
                    }

                    String urlS = "https://vp.gymnasium-odenthal.de/god/" + date;
                    Log.e("LOG", urlS);
                    String authStringEnc = "dnA6Z29kOTIwMQ==";
                    URL url = new URL(urlS);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
                    InputStream is = urlConnection.getInputStream();

                    InputStreamReader isr = new InputStreamReader(is);

                    int numCharsRead;
                    char[] charArray = new char[1024];
                    StringBuffer sb = new StringBuffer();
                    while ((numCharsRead = isr.read(charArray)) > 0) {
                        sb.append(charArray, 0, numCharsRead);
                    }
                    String result = sb.toString();

                    tmp = result;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        /**/

        /* Listener region */
        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(
                        MainActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        mDateSetListener,
                        year,month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;

                if (day >= 10 && month >= 10)
                    date = year + "-" + month + "-" + day;
                else if (day < 10 && month >= 10)
                    date = year + "-" + month + "-0" + day;
                else if (month < 10 && day >= 10)
                    date = year + "-0" + month + "-" + day;
                else
                    date = year + "-0" + month + "-0" + day;
                textView.setText("Deine Vertretungen für : " + day + "." + month + "." + year);
                update();
            }
        };

        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        update();
                        swipe.setRefreshing(false);
                    }
                });
            }
        });
        /**/
    }

    /**
     *
     */
    private void update() {
        try {
            try {
                if (!t.isAlive()) {
                    t.start();
                    Toast.makeText(getApplicationContext(), "Aktualisiere...", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            t.join();

            Toast.makeText(getApplicationContext(), "Aktualisiert!", Toast.LENGTH_SHORT).show();

            Document doc = Jsoup.parse(tmp);
            Log.e("STUFE",Util.getSettingStufe(this));
            filterHTML(doc, Util.getSettingStufe(this));
            tvVers.setText("Version :" + doc.select("strong").first().text());

            Element e = null;

            if(!doc.is("div.alert")) {
                e = doc.select("div.alert").first();
                if(e != null)
                    msgOTD.setText(e.text());
                else
                    msgOTD.setText("An diesem Tag gibt es (noch) keinen Informationstext!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menuheader, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.settings:
                Intent myIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                this.startActivity(myIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void filterHTML(Document d, String stufe) {
        String _stufe = stufe;
        if (_stufe == null || _stufe.equals(""))
            _stufe = "05";

        switch (_stufe){
            case "5":
                _stufe = "05";
                break;
            case "6":
                _stufe = "06";
                break;
            case "7":
                _stufe = "07";
                break;
            case "8":
                _stufe = "08";
                break;
            case "9":
                _stufe = "09";
                break;
        }
        Elements elements = d.select("tr[data-index*='" + _stufe + "']");

        String s = "";

        for(Element e : elements){
            if(e != null)
                s += e.text() + "\n\n";
        }

        txt.setText(s);
    }
}