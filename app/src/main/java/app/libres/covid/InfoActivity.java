package app.libres.covid;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import app.libres.covid.adapter.InfoAdapter;
import app.libres.covid.model.InfoModel;

import static java.util.Objects.requireNonNull;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar toolbar = findViewById(R.id.info_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ListView infoListView = findViewById(R.id.info_list_view);
        InfoAdapter adapter = new InfoAdapter(this);

        infoListView.setAdapter(adapter);
        adapter.addAll(new InfoModel("Viajes", "Recomendaciones de viaje", "Ministerio de Sanidad, Consumo y Bienestar Social", "https://www.mscbs.gob.es/gabinete/notasPrensa.do?metodo=detalle&id=4816"),
                new InfoModel("Prevención", "Consejo de prevención", "Ministerio de Sanidad, Consumo y Bienestar Social", "https://www.mscbs.gob.es/profesionales/saludPublica/ccayes/alertasActual/nCov-China/img/COVID19_como_protegerse.jpg"),
                new InfoModel("Información general", "Información general", "Gobierno de España", "https://www.covid19.gob.es"),
                new InfoModel("Más Información", "Información general", "Ministerio de Sanidad, Consumo y Bienestar Social", "https://www.mscbs.gob.es/profesionales/saludPublica/ccayes/alertasActual/nCov-China/home.htm"),
                new InfoModel("Preguntas frecuentes", "Preguntas frecuentes", "Ministerio de Sanidad, Consumo y Bienestar Social", "https://mscbs.gob.es")
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
